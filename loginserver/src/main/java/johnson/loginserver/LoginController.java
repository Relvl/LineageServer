package johnson.loginserver;

import johnson.loginserver.crypt.ScrambledKeyPair;
import johnson.loginserver.network.gameserverpackets.ServerStatus;
import johnson.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    private static final int BLOWFISH_KEYS = 20;
    private static LoginController _instance;

    private final Map<InetAddress, BanInfo> _bannedIps = new ConcurrentHashMap<>();
    private final Map<InetAddress, FailedLoginAttempt> _hackProtection;
    /** @deprecated see ThreadPoolManager */
    @Deprecated
    private final Thread purgeThread;
    protected Map<String, L2LoginClient> _loginServerClients = new ConcurrentHashMap<>();
    protected ScrambledKeyPair[] _keyPairs;
    protected byte[][] _blowfishKeys;

    private LoginController() throws GeneralSecurityException {
        LOGGER.info("Loading LoginController...");

        _hackProtection = new HashMap<>();

        _keyPairs = new ScrambledKeyPair[10];

        KeyPairGenerator keygen = null;

        keygen = KeyPairGenerator.getInstance("RSA");
        RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
        keygen.initialize(spec);

        // generate the initial set of keys
        for (int i = 0; i < 10; i++)
            _keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());

        LOGGER.info("Cached 10 KeyPairs for RSA communication.");

        testCipher((RSAPrivateKey) _keyPairs[0].getKeyPair().getPrivate());

        // Store keys for blowfish communication
        generateBlowFishKeys();

        purgeThread = new PurgeThread();
        purgeThread.setDaemon(true);
        purgeThread.start();
    }

    public static void load() throws GeneralSecurityException {
        if (_instance == null) {
            _instance = new LoginController();
        }
        else {
            throw new IllegalStateException("LoginController can only be loaded a single time.");
        }
    }

    public static LoginController getInstance() {
        return _instance;
    }

    /**
     * This is mostly to force the initialization of the Crypto Implementation, avoiding it being done on runtime when its first needed.<BR>
     * In short it avoids the worst-case execution time on runtime by doing it on loading.
     *
     * @param key Any private RSA Key just for testing purposes.
     * @throws GeneralSecurityException if a underlying exception was thrown by the Cipher
     */
    private static void testCipher(RSAPrivateKey key) throws GeneralSecurityException {
        // avoid worst-case execution, KenM
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
        rsaCipher.init(Cipher.DECRYPT_MODE, key);
    }

    private void generateBlowFishKeys() {
        _blowfishKeys = new byte[BLOWFISH_KEYS][16];

        for (int i = 0; i < BLOWFISH_KEYS; i++) {
            for (int j = 0; j < _blowfishKeys[i].length; j++)
                _blowfishKeys[i][j] = (byte) (Rnd.get(255) + 1);
        }
        LOGGER.info("Stored {} keys for Blowfish communication.", _blowfishKeys.length);
    }

    /**
     * @return Returns a random key
     */
    public byte[] getBlowfishKey() {
        return _blowfishKeys[(int) (Math.random() * BLOWFISH_KEYS)];
    }

    public SessionKey assignSessionKeyToClient(String account, L2LoginClient client) {
        SessionKey key;

        key = new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt());
        _loginServerClients.put(account, client);
        return key;
    }

    public void removeAuthedLoginClient(String account) {
        if (account == null) {
            return;
        }

        _loginServerClients.remove(account);
    }

    public L2LoginClient getAuthedClient(String account) {
        return _loginServerClients.get(account);
    }

    public EAuthLoginResult tryAuthLogin(String account, String password, L2LoginClient client) {
        EAuthLoginResult ret = EAuthLoginResult.INVALID_PASSWORD;
        // check auth
        if (loginValid(account, password, client)) {
            // login was successful, verify presence on Gameservers
            ret = EAuthLoginResult.ALREADY_ON_GS;
            if (!isAccountInAnyGameServer(account)) {
                // account isnt on any GS verify LS itself
                ret = EAuthLoginResult.ALREADY_ON_LS;

                if (_loginServerClients.put(account, client) == null) {
                    ret = EAuthLoginResult.AUTH_SUCCESS;
                }
            }
        }
        else {
            if (client.getAccessLevel() < 0) {
                ret = EAuthLoginResult.ACCOUNT_BANNED;
            }
        }
        return ret;
    }

    public void addBanForAddress(String address, long expiration) throws UnknownHostException {
        InetAddress netAddress = InetAddress.getByName(address);
        if (!_bannedIps.containsKey(netAddress)) {
            _bannedIps.put(netAddress, new BanInfo(netAddress, expiration));
        }
    }

    public void addBanForAddress(InetAddress address, long duration) {
        if (!_bannedIps.containsKey(address)) {
            _bannedIps.put(address, new BanInfo(address, System.currentTimeMillis() + duration));
        }
    }

    public boolean isBannedAddress(InetAddress address) {
        BanInfo bi = _bannedIps.get(address);
        if (bi != null) {
            if (bi.hasExpired()) {
                _bannedIps.remove(address);
                return false;
            }
            return true;
        }
        return false;
    }

    public Map<InetAddress, BanInfo> getBannedIps() {
        return _bannedIps;
    }

    public boolean removeBanForAddress(InetAddress address) {
        return _bannedIps.remove(address) != null;
    }

    public boolean removeBanForAddress(String address) {
        try {
            return this.removeBanForAddress(InetAddress.getByName(address));
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public SessionKey getKeyForAccount(String account) {
        L2LoginClient client = _loginServerClients.get(account);
        if (client != null) {
            return client.getSessionKey();
        }
        return null;
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
        GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
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
        return _keyPairs[Rnd.get(10)];
    }

    public boolean loginValid(String user, String password, L2LoginClient client) {
        boolean ok = false;
        InetAddress address = client.getConnection().getInetAddress();

        // player disconnected meanwhile
        if (address == null || user == null) {
            return false;
        }

        // FIXME@SQL gameservers
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] raw = password.getBytes("UTF-8");
            byte[] hash = md.digest(raw);

            byte[] expected = null;
            int access = 0;
            int lastServer = 1;

            PreparedStatement statement = con.prepareStatement("SELECT password, access_level, lastServer FROM accounts WHERE login=?");
            statement.setString(1, user);
            ResultSet rset = statement.executeQuery();
            if (rset.next()) {
                expected = Base64.getDecoder().decode(rset.getString("password"));
                access = rset.getInt("access_level");
                lastServer = rset.getInt("lastServer");
                if (lastServer <= 0) {
                    lastServer = 1;
                }
            }
            rset.close();
            statement.close();

            // if account doesnt exists
            if (expected == null) {
                if (LoginServer.config.clientListener.autoCreateAccounts) {
                    if ((user.length() >= 2) && (user.length() <= 14)) {
                        statement = con.prepareStatement("INSERT INTO accounts (login,password,lastactive,access_level) VALUES(?,?,?,?)");
                        statement.setString(1, user);
                        statement.setString(2, Base64.getEncoder().encodeToString(hash));
                        statement.setLong(3, System.currentTimeMillis());
                        statement.setInt(4, 0);
                        statement.execute();
                        statement.close();

                        LOGGER.info("'{}' {} - OK : AccountCreate", user, address.getHostAddress());

                        LOGGER.info("New account has been created for {}.", user);
                        return true;
                    }

                    LOGGER.info("'{}' {} - ERR : ErrCreatingACC", user, address.getHostAddress());

                    LOGGER.warn("Invalid username creation/use attempt: {}.", user);
                    return false;
                }

                LOGGER.info("'{}' {} - ERR : AccountMissing", user, address.getHostAddress());

                LOGGER.warn("Account missing for user {}", user);

                FailedLoginAttempt failedAttempt = _hackProtection.get(address);
                int failedCount;

                if (failedAttempt == null) {
                    _hackProtection.put(address, new FailedLoginAttempt(address, password));
                    failedCount = 1;
                }
                else {
                    failedAttempt.increaseCounter();
                    failedCount = failedAttempt.getCount();
                }

                if (failedCount >= LoginServer.config.clientListener.loginsTryBeforeBan) {
                    LOGGER.info("Banning '{}' for {} seconds due to {} invalid user name attempts", address.getHostAddress(), LoginServer.config.clientListener.loginsBlockAfterBan, failedCount);
                    this.addBanForAddress(address, LoginServer.config.clientListener.loginsBlockAfterBan * 1000);
                }
                return false;
            }

            // is this account banned?
            if (access < 0) {
                LOGGER.info("'{}' {} - ERR : AccountBanned", user, address.getHostAddress());

                client.setAccessLevel(access);
                return false;
            }

            // check password hash
            ok = true;
            for (int i = 0; i < expected.length; i++) {
                if (hash[i] != expected[i]) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                client.setAccessLevel(access);
                client.setLastServer(lastServer);

                PreparedStatement statement2 = con.prepareStatement("UPDATE accounts SET lastactive=? WHERE login=?");
                statement2.setLong(1, System.currentTimeMillis());
                statement2.setString(2, user);
                statement2.execute();
                statement2.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Could not check password:", e);
            ok = false;
        }

        if (!ok) {
            LOGGER.info("'{}' {} - ERR : LoginFailed", user, address.getHostAddress());

            FailedLoginAttempt failedAttempt = _hackProtection.get(address);
            int failedCount;
            if (failedAttempt == null) {
                _hackProtection.put(address, new FailedLoginAttempt(address, password));
                failedCount = 1;
            }
            else {
                failedAttempt.increaseCounter(password);
                failedCount = failedAttempt.getCount();
            }

            if (failedCount >= LoginServer.config.clientListener.loginsTryBeforeBan) {
                LOGGER.info("Banning '{}' for {} seconds due to {} invalid user/pass attempts", address.getHostAddress(), LoginServer.config.clientListener.loginsBlockAfterBan, failedCount);
                this.addBanForAddress(address, LoginServer.config.clientListener.loginsBlockAfterBan * 1000);
            }
        }
        else {
            _hackProtection.remove(address);
            LOGGER.info("'{}' {} - OK : LoginOk", user, address.getHostAddress());
        }
        return ok;
    }

    class FailedLoginAttempt {
        private int _count;
        private long _lastAttempTime;
        private String _lastPassword;

        public FailedLoginAttempt(InetAddress address, String lastPassword) {
            _count = 1;
            _lastAttempTime = System.currentTimeMillis();
            _lastPassword = lastPassword;
        }

        public void increaseCounter(String password) {
            if (!_lastPassword.equals(password)) {
                // check if theres a long time since last wrong try
                if (System.currentTimeMillis() - _lastAttempTime < 300 * 1000) {
                    _count++;
                }
                // restart the status
                else {
                    _count = 1;
                }

                _lastPassword = password;
                _lastAttempTime = System.currentTimeMillis();
            }
            else
            // trying the same password is not brute force
            {
                _lastAttempTime = System.currentTimeMillis();
            }
        }

        public int getCount() {
            return _count;
        }

        public void increaseCounter() {
            _count++;
        }
    }

    class BanInfo {
        private final InetAddress _ipAddress;
        private final long _expiration;

        public BanInfo(InetAddress ipAddress, long expiration) {
            _ipAddress = ipAddress;
            _expiration = expiration;
        }

        public InetAddress getAddress() {
            return _ipAddress;
        }

        public boolean hasExpired() {
            return System.currentTimeMillis() > _expiration && _expiration > 0;
        }
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
                for (L2LoginClient client : _loginServerClients.values()) {
                    if (client == null) {
                        continue;
                    }

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
}