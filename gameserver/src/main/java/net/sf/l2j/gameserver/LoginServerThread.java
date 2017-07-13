package net.sf.l2j.gameserver;

import net.sf.l2j.Config;
import net.sf.l2j.NewCrypt;
import net.sf.l2j.commons.EServerStatus;
import net.sf.l2j.commons.SessionKey;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.GameClientState;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.client.game_to_client.AuthLoginFail;
import net.sf.l2j.gameserver.network.client.game_to_client.CharSelectInfo;
import net.sf.l2j.network.ls_gs_communication.AServerCommunicationThread;
import net.sf.l2j.network.ls_gs_communication.impl.game_to_login.*;
import net.sf.l2j.network.ls_gs_communication.impl.login_to_game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginServerThread extends AServerCommunicationThread {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServerThread.class);
    private static final int REVISION = 0x0102; // FIXME Это в общем-то в конфиг. То же самое, что LoginServer.config.protocolRevision

    private final String hostName;
    private final int port;
    private final int gamePort;
    private final boolean reserveHost;
    private final Collection<WaitingClient> waitingClients = new ArrayList<>();
    private final Map<String, L2GameClient> accountsInGameServer = new ConcurrentHashMap<>();
    private final String gameExternalHost;
    private final String gameInternalHost;
    private final byte[] hexID;
    private final int desiredId;

    private byte[] blowfishKey;
    private Socket socket;
    private EServerStatus serverStatus;

    protected LoginServerThread() {
        super("LoginServerThread");

        port = Config.GAME_SERVER_LOGIN_PORT;
        gamePort = Config.PORT_GAME;
        hostName = Config.GAME_SERVER_LOGIN_HOST;
        hexID = Config.HEX_ID;
        if (hexID == null) {
            LOGGER.error("HexID not found");
        }
        desiredId = 1; // FIXME Config - номер сервера для логинсервера.
        reserveHost = Config.RESERVE_HOST_ON_LOGIN;
        gameExternalHost = Config.EXTERNAL_HOSTNAME;
        gameInternalHost = Config.INTERNAL_HOSTNAME;
    }

    public static byte[] generateHex(int size) {
        return Rnd.nextBytes(new byte[size]);
    }

    public static LoginServerThread getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                // Connection
                LOGGER.info("Connecting to login on {}:{}", hostName, port);

                socket = new Socket(hostName, port);
                inputStream = socket.getInputStream();
                outputStream = new BufferedOutputStream(socket.getOutputStream());

                // init Blowfish
                blowfishKey = generateHex(40);
                blowfishCrypt = new NewCrypt(BLOWFISH_KEY_BASE);

                doThreadLoop(socket);
            } catch (UnknownHostException e) {
                LOGGER.error("", e);
            } catch (IOException ignored) {
                LOGGER.info("No connection found with loginserver, next try in 10 seconds.");
            } finally {
                try {
                    socket.close();
                    isInterrupted();
                } catch (RuntimeException | IOException ignored) { }
            }

            // 10 seconds tempo before another try
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) { }
        }
    }

    @Override
    protected void doProcessIncomindData(int packetType, byte[] incoming) {
        switch (packetType) {
            case 0x00: // InitLSPacket
                InitGameServerPacket init = new InitGameServerPacket(incoming);
                if (init.getRevision() != REVISION) {
                    LOGGER.warn("/!\\ Revision mismatch between LS({}) and GS({}) /!\\", init.getRevision(), REVISION);
                    break;
                }

                try {
                    KeyFactory kfac = KeyFactory.getInstance("RSA");
                    BigInteger modulus = new BigInteger(init.getPublicKey());
                    publicKey = (RSAPublicKey) kfac.generatePublic(new RSAPublicKeySpec(modulus, RSAKeyGenParameterSpec.F4));
                } catch (GeneralSecurityException e) {
                    LOGGER.warn("Troubles while init the public key send by login", e);
                    break;
                }

                // send the blowfish key through the rsa encryption
                BlowFishKeyInitPacket bfk = new BlowFishKeyInitPacket(blowfishKey, publicKey);
                sendPacket(bfk);

                // now, only accept paket with the new encryption
                blowfishCrypt = new NewCrypt(blowfishKey);

                GameServerAuthRequestPacket ar = new GameServerAuthRequestPacket();
                ar.setDesiredId(desiredId);
                ar.setHexId(hexID);
                ar.setExternalHost(gameExternalHost);
                ar.setInternalHost(gameInternalHost);
                ar.setPort(gamePort);
                ar.setHostReserved(reserveHost);
                ar.setMaxPlayers(Config.MAXIMUM_ONLINE_USERS);
                sendPacket(ar);
                break;
            case 0x01: // LoginFailPacket
                GameServerLoginFailPacket lsf = new GameServerLoginFailPacket(incoming);
                LOGGER.info("Registeration Failed: {}", lsf.getReason().getText());
                break;
            case 0x02: // AuthResponsePacket
                LoginServerAuthResponsePacket aresp = new LoginServerAuthResponsePacket(incoming);
                LOGGER.info("Registered on login as server: [{}] ", aresp.getServerId());
                sendServerStatus(EServerStatus.STATUS_AUTO);
                Collection<L2PcInstance> players = L2World.getInstance().getPlayers();
                if (!players.isEmpty()) {
                    List<String> playerList = new ArrayList<>();
                    for (L2PcInstance player : players) { playerList.add(player.getAccountName()); }

                    sendPacket(new PlayerInGameServerPacket(playerList));
                }
                break;
            case 0x03: // PlayerAuthResponsePacket
                PlayerAuthResponsePacket par = new PlayerAuthResponsePacket(incoming);
                String account = par.getLogin();
                WaitingClient wcToRemove = null;
                synchronized (waitingClients) {
                    for (WaitingClient wc : waitingClients) {
                        if (wc.account.equals(account)) {
                            wcToRemove = wc;
                        }
                    }
                }

                if (wcToRemove != null) {
                    if (par.isAuthed()) {
                        PlayerInGameServerPacket pig = new PlayerInGameServerPacket(par.getLogin());
                        sendPacket(pig);
                        wcToRemove.gameClient.setState(GameClientState.AUTHED);
                        wcToRemove.gameClient.setSessionId(wcToRemove.session);
                        CharSelectInfo cl = new CharSelectInfo(wcToRemove.account, wcToRemove.gameClient.getSessionId().getPlayOkID1());
                        wcToRemove.gameClient.getConnection().sendPacket(cl);
                        wcToRemove.gameClient.setCharSelection(cl.getCharInfo());
                    }
                    else {
                        LOGGER.warn("Session key is not correct. closing connection");
                        wcToRemove.gameClient.getConnection().sendPacket(new AuthLoginFail(1));
                        wcToRemove.gameClient.closeNow();
                    }
                    waitingClients.remove(wcToRemove);
                }
                break;
            case 0x04: // KickPlayerPacket
                KickPlayerFromGamePacket kp = new KickPlayerFromGamePacket(incoming);
                doKickPlayer(kp.getLogin());
                break;
        }
    }

    public void addWaitingClientAndSendRequest(String acc, L2GameClient client, SessionKey key) {
        WaitingClient wc = new WaitingClient(acc, client, key);
        synchronized (waitingClients) {
            waitingClients.add(wc);
        }
        sendPacket(new PlayerAuthRequestPacket(acc, key));
    }

    public void sendLogout(String account) {
        if (account == null) {
            return;
        }
        sendPacket(new PlayerLogoutFromGamePacket(account));
        accountsInGameServer.remove(account);
    }

    public boolean addGameServerLogin(String account, L2GameClient client) {
        if (accountsInGameServer.containsKey(account)) {
            return false;
        }

        return accountsInGameServer.put(account, client) == null;
    }

    public void doKickPlayer(String account) {
        if (accountsInGameServer.get(account) != null) {
            accountsInGameServer.get(account).closeNow();
            getInstance().sendLogout(account);
        }
    }

    public void sendServerStatus(EServerStatus status) {
        GameServerStatusPacket statusPacket = new GameServerStatusPacket();
        statusPacket.setIsBracket(Config.SERVER_LIST_BRACKET);
        statusPacket.setIsClock(Config.SERVER_LIST_CLOCK);
        statusPacket.setIsTestServer(Config.SERVER_LIST_TESTSERVER);
        statusPacket.setMaxPlayers(Config.MAXIMUM_ONLINE_USERS);
        statusPacket.setServerStatus(status);
        sendPacket(statusPacket);
    }

    public String getStatusString() {
        return serverStatus.getText();
    }

    public EServerStatus getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(EServerStatus status) {
        this.serverStatus = status;
        sendServerStatus(status);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    private static final class SingletonHolder {
        private static final LoginServerThread INSTANCE = new LoginServerThread();
    }

    private static final class WaitingClient {
        public String account;
        public L2GameClient gameClient;
        public SessionKey session;

        private WaitingClient(String acc, L2GameClient client, SessionKey key) {
            account = acc;
            gameClient = client;
            session = key;
        }
    }

}