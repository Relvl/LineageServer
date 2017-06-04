package johnson.loginserver;

import johnson.loginserver.crypt.ScrambledKeyPair;
import johnson.loginserver.database.CreateAccountCall;
import johnson.loginserver.database.LoginCall;
import johnson.loginserver.network.client.login_to_client.List;
import johnson.loginserver.network.gameserver.game_to_login.ServerStatus;
import johnson.loginserver.network.client.login_to_client.LoginFail.LoginFailReason;
import johnson.loginserver.network.client.login_to_client.LoginOk;
import johnson.loginserver.security.SecurityController;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.database.CallException;
import net.sf.l2j.commons.random.Rnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    private static final int BLOWFISH_KEYS = 20;

    /** @deprecated see ThreadPoolManager */
    @Deprecated
    private final Thread purgeThread;

    protected Map<String, L2LoginClient> clients = new ConcurrentHashMap<>();
    protected ScrambledKeyPair[] keyPairs;
    protected byte[][] blowfishKeys;

    private LoginController() {
        try {
            this.keyPairs = new ScrambledKeyPair[10];
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            keygen.initialize(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4));
            for (int i = 0; i < 10; i++)
                this.keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());
            Cipher.getInstance("RSA/ECB/nopadding").init(Cipher.DECRYPT_MODE, this.keyPairs[0].getKeyPair().getPrivate());

            this.blowfishKeys = new byte[BLOWFISH_KEYS][16];
            for (int i = 0; i < BLOWFISH_KEYS; i++) {
                for (int j = 0; j < this.blowfishKeys[i].length; j++)
                    this.blowfishKeys[i][j] = (byte) (Rnd.get(255) + 1);
            }
        } catch (GeneralSecurityException e) {
            LOGGER.error("Failed to make security keys.", e);
            throw new RuntimeException(e);
        }

        purgeThread = new PurgeThread();
        purgeThread.setDaemon(true);
        purgeThread.start();
    }

    public byte[] getBlowfishKey() {
        return blowfishKeys[(int) (Math.random() * BLOWFISH_KEYS)];
    }

    /** Проверка логина/пароля, банов и наличие этого аккаунта в игре. */
    public void onAuthLogin(String login, String password, L2LoginClient client) {
        InetAddress address = client.getConnection().getInetAddress();

        try (LoginCall call = new LoginCall(login, password)) {
            call.execute();

            // Пароль не верный.
            if (call.getAccountId() == null) {
                SecurityController.getInstance().handleIncorrectLognis(address, password);
                client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
                return;
            }

            // Пароль верный.
            if (call.getAccountId() > 0) {
                // TODO Проверка на баны.
                // client.close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));

                // Аккаунт уже в игре.
                if (isAccountInAnyGameServer(login)) {
                    // Выкидываем старого клиента.
                    GameServerInfo gsi;
                    if ((gsi = getAccountOnGameServer(login)) != null) {
                        client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
                        if (gsi.isAuthed()) {
                            gsi.getGameServerThread().kickPlayer(login);
                        }
                    }
                    return;
                }

                // Аккаунт уже прошел авторизацию ранее. TODO Как минимум странно...
                if (clients.put(login, client) != null) {
                    // Выкидываем старого клиента.
                    L2LoginClient oldClient;
                    if ((oldClient = getClient(login)) != null) {
                        oldClient.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
                        clients.remove(login);
                    }
                    client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
                    return;
                }

                onAuthLoginSuccess(client, login, call.getLastServer());
                return;
            }

            // Аккант не существует.
            if (call.getAccountId() < 0) {
                if (LoginServer.config.clientListener.autoCreateAccounts) {
                    try (CreateAccountCall createAccountCall = new CreateAccountCall(login, password)) {
                        call.execute();
                        // Аккаунт создан
                        if (createAccountCall.getAccountId() > 0) {
                            onAuthLoginSuccess(client, login, 0);
                            return;
                        }
                        // Не удалось создать аккаунт.
                        else {
                            LOGGER.warn("Failed to create new account '{}'.", login);
                        }
                    }
                }
            }

        } catch (CallException e) {
            LOGGER.warn("Could not check password ({}):", login, e);
        }

        SecurityController.getInstance().handleIncorrectLognis(address, password);
        client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
    }

    private void onAuthLoginSuccess(L2LoginClient client, String login, int lastServer) {
        SecurityController.getInstance().handleCorrectLogin(client.getConnection().getInetAddress());

        client.setAccount(login);
        client.setSessionKey(new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt()));
        client.setState(ELoginClientState.AUTHED_LOGIN);
        client.setAccessLevel(0);
        client.setLastServer(lastServer);

        client.sendPacket(LoginServer.config.clientListener.showLicense ?
                        new LoginOk(client.getSessionKey()) :
                        new List(client)
        );

        clients.put(login, client);
    }

    public void removeClient(String login) {
        if (login == null) { return; }
        clients.remove(login);
    }

    public L2LoginClient getClient(String login) {
        return clients.get(login);
    }

    public SessionKey getKeyForAccount(String account) {
        return clients.containsKey(account) ? clients.get(account).getSessionKey() : null;
    }

    public boolean isAccountInAnyGameServer(String account) {
        Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
        for (GameServerInfo gsi : serverList) {
            GameServerThread gst = gsi.getGameServerThread();
            if (gst != null && gst.hasAccountOnGameServer(account)) {
                return true;
            }
        }
        return false;
    }

    public GameServerInfo getAccountOnGameServer(String account) {
        Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
        for (GameServerInfo gsi : serverList) {
            GameServerThread gst = gsi.getGameServerThread();
            if (gst != null && gst.hasAccountOnGameServer(account)) {
                return gsi;
            }
        }
        return null;
    }

    public boolean isLoginPossible(L2LoginClient client, int serverId) {
        GameServerInfo gsi = GameServerTable.getInstance().getGameServer(serverId);
        int access = client.getAccessLevel();
        if (gsi != null && gsi.isAuthed()) {
            boolean loginOk = (gsi.getCurrentPlayerCount() < gsi.getMaxPlayers() && gsi.getStatus() != ServerStatus.STATUS_GM_ONLY) || access > 0;

            if (loginOk && client.getLastServer() != serverId) {
                // FIXME@SQL gameservers
                try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
                    PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lastServer = ? WHERE login = ?");
                    statement.setInt(1, serverId);
                    statement.setString(2, client.getAccount());
                    statement.executeUpdate();
                    statement.close();
                } catch (Exception e) {
                    LOGGER.warn("Could not set lastServer: {}", e.getMessage(), e);
                }
            }
            return loginOk;
        }
        return false;
    }

    public void setAccountAccessLevel(String account, int banLevel) {
        // FIXME@SQL gameservers
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE accounts SET access_level=? WHERE login=?");
            statement.setInt(1, banLevel);
            statement.setString(2, account);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            LOGGER.warn("Could not set accessLevel: {}", e.getMessage(), e);
        }
    }

    public ScrambledKeyPair getScrambledRSAKeyPair() {
        return keyPairs[Rnd.get(10)];
    }

    public static LoginController getInstance() {
        return SingletonHolder.instance;
    }

    /** @deprecated Заменить на ThreadPoolManager. */
    @Deprecated
    class PurgeThread extends Thread {
        public PurgeThread() {
            setName("PurgeThread");
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                for (L2LoginClient client : clients.values()) {
                    if (client == null) { continue; }

                    if ((client.getLoginTimestamp() + LoginServer.config.clientListener.loginTimeout) < System.currentTimeMillis()) {
                        client.close(LoginFailReason.REASON_ACCESS_FAILED);
                    }
                }

                try {
                    Thread.sleep(LoginServer.config.clientListener.loginTimeout / 2);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private static final class SingletonHolder {
        private static final LoginController instance = new LoginController();
    }
}