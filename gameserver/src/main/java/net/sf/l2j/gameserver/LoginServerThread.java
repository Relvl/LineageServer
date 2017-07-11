package net.sf.l2j.gameserver;

import net.sf.l2j.Config;
import net.sf.l2j.NewCrypt;
import net.sf.l2j.commons.EServerStatus;
import net.sf.l2j.commons.lang.HexUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.GameClientState;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.gameserverpackets.*;
import net.sf.l2j.gameserver.network.loginserverpackets.*;
import net.sf.l2j.gameserver.network.serverpackets.AuthLoginFail;
import net.sf.l2j.gameserver.network.serverpackets.CharSelectInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

public class LoginServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServerThread.class);

    private static final int REVISION = 0x0102;
    private final String _hostname;
    private final int _port;
    private final int _gamePort;
    private final boolean _reserveHost;
    private final List<WaitingClient> _waitingClients;
    private final Map<String, L2GameClient> _accountsInGameServer;
    private final String _gameExternalHost;
    private final String _gameInternalHost;
    private RSAPublicKey _publicKey;
    private Socket _loginSocket;
    private InputStream _in;
    private OutputStream _out;
    /**
     * The BlowFish engine used to encrypt packets<br>
     * It is first initialized with a unified key:<br>
     * "_;v.]05-31!|+-%xT!^[$\00"<br>
     * <br>
     * and then after handshake, with a new key sent by<br>
     * loginserver during the handshake. This new key is stored<br>
     * in {@link #_blowfishKey}
     */
    private NewCrypt _blowfish;
    private byte[] _blowfishKey;
    private byte[] _hexID;
    private int _desiredId;
    private int _serverID;
    private EServerStatus serverStatus;

    protected LoginServerThread() {
        super("LoginServerThread");
        _port = Config.GAME_SERVER_LOGIN_PORT;
        _gamePort = Config.PORT_GAME;
        _hostname = Config.GAME_SERVER_LOGIN_HOST;
        _hexID = Config.HEX_ID;
        _desiredId = 1; // FIXME Config - номер сервера для логинсервера.
        if (_hexID == null) {
            LOGGER.error("HexID not found");
        }
        _reserveHost = Config.RESERVE_HOST_ON_LOGIN;
        _gameExternalHost = Config.EXTERNAL_HOSTNAME;
        _gameInternalHost = Config.INTERNAL_HOSTNAME;
        _waitingClients = new ArrayList<>();
        _accountsInGameServer = new ConcurrentHashMap<>();
    }

    public static LoginServerThread getInstance() {
        return SingletonHolder._instance;
    }

    private static String hexToString(byte[] hex) {
        return new BigInteger(hex).toString(16);
    }

    public static byte[] generateHex(int size) {
        byte[] array = new byte[size];
        Rnd.nextBytes(array);

        if (Config.DEBUG) {
            LOGGER.info("Generated random String:  \"" + array + "\"");
        }

        return array;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                // Connection
                LOGGER.info("Connecting to login on {}:{}", _hostname, _port);
                _loginSocket = new Socket(_hostname, _port);
                _in = _loginSocket.getInputStream();
                _out = new BufferedOutputStream(_loginSocket.getOutputStream());

                // init Blowfish
                _blowfishKey = generateHex(40);
                _blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
                while (!isInterrupted()) {
                    int lengthLo = _in.read();
                    int lengthHi = _in.read();
                    int length = lengthHi * 256 + lengthLo;

                    if (lengthHi < 0) {
                        LOGGER.info("LoginServerThread: Login terminated the connection.");
                        break;
                    }

                    byte[] incoming = new byte[length - 2];

                    int receivedBytes = 0;
                    int newBytes = 0;
                    int left = length - 2;

                    while (newBytes != -1 && receivedBytes < length - 2) {
                        newBytes = _in.read(incoming, receivedBytes, left);
                        receivedBytes = receivedBytes + newBytes;
                        left -= newBytes;
                    }

                    if (receivedBytes != length - 2) {
                        LOGGER.warn("Incomplete Packet is sent to the server, closing connection.(LS)");
                        break;
                    }

                    // decrypt if we have a key
                    byte[] decrypt = _blowfish.decrypt(incoming);
                    boolean checksumOk = NewCrypt.verifyChecksum(decrypt);

                    if (!checksumOk) {
                        LOGGER.warn("Incorrect packet checksum, ignoring packet (LS)");
                        break;
                    }

                    if (Config.DEBUG) {
                        LOGGER.warn("[C]\n{}", HexUtil.printData(decrypt));
                    }

                    int packetType = decrypt[0] & 0xff;
                    switch (packetType) {
                        case 0x00:
                            InitLS init = new InitLS(decrypt);
                            if (Config.DEBUG) {
                                LOGGER.info("Init received");
                            }

                            if (init.getRevision() != REVISION) {
                                LOGGER.warn("/!\\ Revision mismatch between LS and GS /!\\");
                                break;
                            }

                            try {
                                KeyFactory kfac = KeyFactory.getInstance("RSA");
                                BigInteger modulus = new BigInteger(init.getRSAKey());
                                RSAPublicKeySpec kspec1 = new RSAPublicKeySpec(modulus, RSAKeyGenParameterSpec.F4);
                                _publicKey = (RSAPublicKey) kfac.generatePublic(kspec1);
                                if (Config.DEBUG) {
                                    LOGGER.info("RSA key set up");
                                }
                            } catch (GeneralSecurityException e) {
                                LOGGER.warn("Troubles while init the public key send by login");
                                break;
                            }

                            // send the blowfish key through the rsa encryption
                            BlowFishKey bfk = new BlowFishKey(_blowfishKey, _publicKey);
                            sendPacket(bfk);

                            if (Config.DEBUG) {
                                LOGGER.info("Sent new blowfish key");
                            }

                            // now, only accept paket with the new encryption
                            _blowfish = new NewCrypt(_blowfishKey);
                            if (Config.DEBUG) {
                                LOGGER.info("Changed blowfish key");
                            }

                            GameServerAuthRequestPacket ar = new GameServerAuthRequestPacket(_desiredId, _hexID, _gameExternalHost, _gameInternalHost, _gamePort, _reserveHost, Config.MAXIMUM_ONLINE_USERS);
                            sendPacket(ar);
                            if (Config.DEBUG) {
                                LOGGER.info("Sent GameServerAuthRequestPacket to login");
                            }
                            break;
                        case 0x01:
                            LoginServerFail lsf = new LoginServerFail(decrypt);
                            LOGGER.info("Damn! Registeration Failed: " + lsf.getReasonString());
                            // login will close the connection here
                            break;
                        case 0x02:
                            AuthResponse aresp = new AuthResponse(decrypt);
                            _serverID = aresp.getServerId();
                            LOGGER.info("Registered on login as server: [" + _serverID + "] ");

                            sendServerStatus(EServerStatus.STATUS_AUTO);

                            final Collection<L2PcInstance> players = L2World.getInstance().getPlayers();
                            if (!players.isEmpty()) {
                                final List<String> playerList = new ArrayList<>();
                                for (L2PcInstance player : players) { playerList.add(player.getAccountName()); }

                                sendPacket(new PlayerInGame(playerList));
                            }
                            break;
                        case 0x03:
                            PlayerAuthResponse par = new PlayerAuthResponse(decrypt);
                            String account = par.getAccount();
                            WaitingClient wcToRemove = null;
                            synchronized (_waitingClients) {
                                for (WaitingClient wc : _waitingClients) {
                                    if (wc.account.equals(account)) {
                                        wcToRemove = wc;
                                    }
                                }
                            }

                            if (wcToRemove != null) {
                                if (par.isAuthed()) {
                                    PlayerInGame pig = new PlayerInGame(par.getAccount());
                                    sendPacket(pig);
                                    wcToRemove.gameClient.setState(GameClientState.AUTHED);
                                    wcToRemove.gameClient.setSessionId(wcToRemove.session);
                                    CharSelectInfo cl = new CharSelectInfo(wcToRemove.account, wcToRemove.gameClient.getSessionId().playOkID1);
                                    wcToRemove.gameClient.getConnection().sendPacket(cl);
                                    wcToRemove.gameClient.setCharSelection(cl.getCharInfo());
                                }
                                else {
                                    LOGGER.warn("Session key is not correct. closing connection");
                                    wcToRemove.gameClient.getConnection().sendPacket(new AuthLoginFail(1));
                                    wcToRemove.gameClient.closeNow();
                                }
                                _waitingClients.remove(wcToRemove);
                            }
                            break;
                        case 0x04:
                            KickPlayer kp = new KickPlayer(decrypt);
                            doKickPlayer(kp.getAccount());
                            break;
                    }
                }
            } catch (UnknownHostException e) {
                if (Config.DEBUG) {
                    LOGGER.warn("", e);
                }
            } catch (IOException e) {
                LOGGER.info("No connection found with loginserver, next try in 10 seconds.");
            } finally {
                try {
                    _loginSocket.close();
                    if (isInterrupted()) {
                        return;
                    }
                } catch (Exception e) {
                }
            }

            // 10 seconds tempo before another try
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void addWaitingClientAndSendRequest(String acc, L2GameClient client, SessionKey key) {
        if (Config.DEBUG) {
            LOGGER.info(String.valueOf(key));
        }

        WaitingClient wc = new WaitingClient(acc, client, key);
        synchronized (_waitingClients) {
            _waitingClients.add(wc);
        }
        PlayerAuthRequest par = new PlayerAuthRequest(acc, key);

        try {
            sendPacket(par);
        } catch (IOException e) {
            LOGGER.warn("Error while sending player auth request");
            if (Config.DEBUG) {
                LOGGER.warn("", e);
            }
        }
    }

    public void removeWaitingClient(L2GameClient client) {
        WaitingClient toRemove = null;
        synchronized (_waitingClients) {
            for (WaitingClient c : _waitingClients) {
                if (c.gameClient == client) {
                    toRemove = c;
                }
            }

            if (toRemove != null) {
                _waitingClients.remove(toRemove);
            }
        }
    }

    public void sendLogout(String account) {
        if (account == null) {
            return;
        }

        PlayerLogout pl = new PlayerLogout(account);
        try {
            sendPacket(pl);
        } catch (IOException e) {
            LOGGER.warn("Error while sending logout packet to login");
            if (Config.DEBUG) {
                LOGGER.warn("", e);
            }
        } finally {
            _accountsInGameServer.remove(account);
        }
    }

    public boolean addGameServerLogin(String account, L2GameClient client) {
        if (_accountsInGameServer.containsKey(account)) {
            return false;
        }

        return _accountsInGameServer.put(account, client) == null;
    }

    public void sendAccessLevel(String account, int level) {
        ChangeAccessLevel cal = new ChangeAccessLevel(account, level);
        try {
            sendPacket(cal);
        } catch (IOException e) {
            if (Config.DEBUG) {
                LOGGER.warn("", e);
            }
        }
    }

    public void doKickPlayer(String account) {
        if (_accountsInGameServer.get(account) != null) {
            _accountsInGameServer.get(account).closeNow();
            LoginServerThread.getInstance().sendLogout(account);
        }
    }

    private void sendPacket(GameServerBasePacket sl) throws IOException {
        byte[] data = sl.getContent();
        NewCrypt.appendChecksum(data);
        if (Config.DEBUG) {
            LOGGER.info("[S]\n{}", HexUtil.printData(data));
        }

        data = _blowfish.crypt(data);

        int len = data.length + 2;
        synchronized (_out) // avoids tow threads writing in the mean time
        {
            _out.write(len & 0xff);
            _out.write(len >> 8 & 0xff);
            _out.write(data);
            _out.flush();
        }
    }

    public void sendServerStatus(EServerStatus status) {
        ServerStatusPacket statusPacket = new ServerStatusPacket();
        statusPacket.setIsBracket(Config.SERVER_LIST_BRACKET);
        statusPacket.setIsClock(Config.SERVER_LIST_CLOCK);
        statusPacket.setIsTestServer(Config.SERVER_LIST_TESTSERVER);
        statusPacket.setMaxPlayers(Config.MAXIMUM_ONLINE_USERS);
        statusPacket.setServerStatus(status);
        try {
            sendPacket(statusPacket);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    public String getStatusString() {
        return serverStatus.getText();
    }

    public boolean isClockShown() {
        return Config.SERVER_LIST_CLOCK;
    }

    public boolean isBracketShown() {
        return Config.SERVER_LIST_BRACKET;
    }

    public EServerStatus getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(EServerStatus status) {
        this.serverStatus = status;
        sendServerStatus(status);
    }

    public static class SessionKey {
        public int playOkID1;
        public int playOkID2;
        public int loginOkID1;
        public int loginOkID2;

        public SessionKey(int loginOK1, int loginOK2, int playOK1, int playOK2) {
            playOkID1 = playOK1;
            playOkID2 = playOK2;
            loginOkID1 = loginOK1;
            loginOkID2 = loginOK2;
        }

        @Override
        public String toString() {
            return "PlayOk: " + playOkID1 + " " + playOkID2 + " LoginOk:" + loginOkID1 + " " + loginOkID2;
        }
    }

    private static class SingletonHolder {
        protected static final LoginServerThread _instance = new LoginServerThread();
    }

    private class WaitingClient {
        public String account;
        public L2GameClient gameClient;
        public SessionKey session;

        public WaitingClient(String acc, L2GameClient client, SessionKey key) {
            account = acc;
            gameClient = client;
            session = key;
        }
    }
}