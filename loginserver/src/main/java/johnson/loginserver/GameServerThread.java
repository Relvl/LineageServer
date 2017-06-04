package johnson.loginserver;

import johnson.loginserver.network.gameserver.game_to_login.*;
import johnson.loginserver.network.gameserver.login_to_game.*;
import johnson.loginserver.network.gameserver.ABaseServerPacket;
import johnson.loginserver.security.SecurityController;
import net.sf.l2j.NewCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GameServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServerThread.class);

    private final Socket _connection;
    private final RSAPublicKey _publicKey;
    private final RSAPrivateKey _privateKey;
    private final String _connectionIp;
    /**
     * Authed Clients on a GameServer
     */
    private final Set<String> _accountsOnGameServer = new HashSet<>();
    private InputStream _in;
    private OutputStream _out;
    private NewCrypt _blowfish;
    private byte[] _blowfishKey;
    private GameServerInfo _gsi;
    private String _connectionIPAddress;

    public GameServerThread(Socket con) {
        _connection = con;
        _connectionIp = con.getInetAddress().getHostAddress();
        try {
            _in = _connection.getInputStream();
            _out = new BufferedOutputStream(_connection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        KeyPair pair = GameServerTable.getInstance().getRandomKeyPair();
        _privateKey = (RSAPrivateKey) pair.getPrivate();
        _publicKey = (RSAPublicKey) pair.getPublic();
        _blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
        start();
    }

    /**
     * @param ipAddress
     * @return true if the given IP is banned.
     */
    public static boolean isBannedGameserverIP(String ipAddress) {
        InetAddress netAddress = null;
        try {
            netAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return SecurityController.getInstance().isBannedAddress(netAddress);
    }

    @Override
    public void run() {
        _connectionIPAddress = _connection.getInetAddress().getHostAddress();

        // Ensure no further processing for this connection if server is considered as banned.
        if (GameServerThread.isBannedGameserverIP(_connectionIPAddress)) {
            LOGGER.info("GameServer with banned IP {} tries to register.", _connectionIPAddress);
            forceClose(LoginAFailServerPacket.REASON_IP_BANNED);
            return;
        }

        try {
            sendPacket(new InitLSPacket(_publicKey.getModulus().toByteArray()));

            int lengthHi = 0;
            int lengthLo = 0;
            int length = 0;
            boolean checksumOk = false;
            for (; ; ) {
                lengthLo = _in.read();
                lengthHi = _in.read();
                length = lengthHi * 256 + lengthLo;

                if (lengthHi < 0 || _connection.isClosed()) {
                    break;
                }

                byte[] data = new byte[length - 2];

                int receivedBytes = 0;
                int newBytes = 0;
                while (newBytes != -1 && receivedBytes < length - 2) {
                    newBytes = _in.read(data, 0, length - 2);
                    receivedBytes = receivedBytes + newBytes;
                }

                if (receivedBytes != length - 2) {
                    LOGGER.warn("Incomplete packet is sent to the server, closing connection.");
                    break;
                }

                // decrypt if we have a key
                data = _blowfish.decrypt(data);
                checksumOk = NewCrypt.verifyChecksum(data);
                if (!checksumOk) {
                    LOGGER.warn("Incorrect packet checksum, closing connection.");
                    return;
                }

                int packetType = data[0] & 0xff;
                switch (packetType) {
                    case 00:
                        onReceiveBlowfishKey(data);
                        break;

                    case 01:
                        onGameServerAuth(data);
                        break;

                    case 02:
                        onReceivePlayerInGame(data);
                        break;

                    case 03:
                        onReceivePlayerLogOut(data);
                        break;

                    case 04:
                        onReceiveChangeAccessLevel(data);
                        break;

                    case 05:
                        onReceivePlayerAuthRequest(data);
                        break;

                    case 06:
                        onReceiveServerStatus(data);
                        break;

                    default:
                        LOGGER.warn("Unknown Opcode (" + Integer.toHexString(packetType).toUpperCase() + ") from GameServer, closing connection.");
                        forceClose(LoginAFailServerPacket.NOT_AUTHED);
                }

            }
        } catch (IOException e) {
            String serverName = (getServerId() != -1 ? "[" + getServerId() + "] " : "(" + _connectionIPAddress + ")");
            LOGGER.info("GameServer {}: {}.", serverName, e.getMessage());
        } finally {
            if (isAuthed()) {
                _gsi.setDown();
                LOGGER.info("GameServer [{}]  is now set as disconnected.", getServerId());
            }
            LoginServer.getInstance().getGameServerListener().removeGameServer(this);
            LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
        }
    }

    private void onReceiveBlowfishKey(byte[] data) {
        final BlowFishKeyPacket bfk = new BlowFishKeyPacket(data, _privateKey);

        _blowfishKey = bfk.getKey();
        _blowfish = new NewCrypt(_blowfishKey);
    }

    private void onGameServerAuth(byte[] data) {
        handleRegProcess(new GameServerAuthPacket(data));

        if (isAuthed()) {
            sendPacket(new AuthResponsePacket(_gsi.getId()));
        }
    }

    private void onReceivePlayerInGame(byte[] data) {
        if (isAuthed()) {
            final PlayerInGamePacket pig = new PlayerInGamePacket(data);

            for (String account : pig.getAccounts())
                _accountsOnGameServer.add(account);
        }
        else {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
        }
    }

    private void onReceivePlayerLogOut(byte[] data) {
        if (isAuthed()) {
            final PlayerLogoutPacket plo = new PlayerLogoutPacket(data);

            _accountsOnGameServer.remove(plo.getAccount());
        }
        else {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
        }
    }

    private void onReceiveChangeAccessLevel(byte[] data) {
        if (isAuthed()) {
            final ChangeAccessLevelPacket cal = new ChangeAccessLevelPacket(data);

            LoginController.getInstance().setAccountAccessLevel(cal.getAccount(), cal.getLevel());
            LOGGER.info("Changed {} access level to {}.", cal.getAccount(), cal.getLevel());
        }
        else {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
        }
    }

    private void onReceivePlayerAuthRequest(byte[] data) {
        if (isAuthed()) {
            final PlayerAuthRequestPacket par = new PlayerAuthRequestPacket(data);
            final SessionKey key = LoginController.getInstance().getKeyForAccount(par.getAccount());

            if (key != null && key.equals(par.getKey())) {
                LoginController.getInstance().removeClient(par.getAccount());
                sendPacket(new PlayerAuthResponsePacket(par.getAccount(), true));
            }
            else {
                sendPacket(new PlayerAuthResponsePacket(par.getAccount(), false));
            }
        }
        else {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
        }
    }

    private void onReceiveServerStatus(byte[] data) {
        if (isAuthed()) {
            new ServerStatusPacket(data, getServerId()); // will do the actions by itself
        }
        else {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
        }
    }

    private void handleRegProcess(GameServerAuthPacket gameServerAuthPacket) {
        final int id = gameServerAuthPacket.getDesiredID();
        final byte[] hexId = gameServerAuthPacket.getHexID();

        GameServerInfo gsi = GameServerTable.getInstance().getGameServer(id);
        // is there a gameserver registered with this id?
        if (gsi != null) {
            // does the hex id match?
            if (Arrays.equals(gsi.getHexId(), hexId)) {
                // check to see if this GS is already connected
                synchronized (gsi) {
                    if (gsi.isAuthed()) {
                        forceClose(LoginAFailServerPacket.REASON_ALREADY_LOGGED8IN);
                    }
                    else {
                        attachGameServerInfo(gsi, gameServerAuthPacket);
                    }
                }
            }
            else {
                forceClose(LoginAFailServerPacket.REASON_WRONG_HEXID);
            }
        }
        else {
            forceClose(LoginAFailServerPacket.REASON_WRONG_HEXID);
        }
    }

    public boolean hasAccountOnGameServer(String account) {
        return _accountsOnGameServer.contains(account);
    }

    public int getPlayerCount() {
        return _accountsOnGameServer.size();
    }

    /**
     * Attachs a GameServerInfo to this Thread <li>Updates the GameServerInfo values based on GameServerAuthPacket packet</li> <li><b>Sets the GameServerInfo as Authed</b></li>
     *
     * @param gsi            The GameServerInfo to be attached.
     * @param gameServerAuthPacket The server info.
     */
    private void attachGameServerInfo(GameServerInfo gsi, GameServerAuthPacket gameServerAuthPacket) {
        setGameServerInfo(gsi);
        gsi.setGameServerThread(this);
        gsi.setPort(gameServerAuthPacket.getPort());
        setGameHosts(gameServerAuthPacket.getExternalHost(), gameServerAuthPacket.getInternalHost());
        gsi.setMaxPlayers(gameServerAuthPacket.getMaxPlayers());
        gsi.setAuthed(true);
    }

    private void forceClose(int reason) {
        sendPacket(new LoginAFailServerPacket(reason));

        try {
            _connection.close();
        } catch (IOException e) {
            LOGGER.warn("GameServerThread: Failed disconnecting banned server, server already disconnected.");
        }
    }

    /**
     * @param sl
     */
    private void sendPacket(ABaseServerPacket sl) {
        try {
            byte[] data = sl.getContent();
            NewCrypt.appendChecksum(data);
            data = _blowfish.crypt(data);

            int len = data.length + 2;
            synchronized (_out) {
                _out.write(len & 0xff);
                _out.write(len >> 8 & 0xff);
                _out.write(data);
                _out.flush();
            }
        } catch (IOException e) {
            LOGGER.error("IOException while sending packet {}.", sl.getClass().getSimpleName());
        }
    }

    public void kickPlayer(String account) {
        sendPacket(new KickPlayerPacket(account));
    }

    /**
     * @param gameExternalHost
     * @param gameInternalHost
     */
    public void setGameHosts(String gameExternalHost, String gameInternalHost) {
        String oldInternal = _gsi.getInternalHost();
        String oldExternal = _gsi.getExternalHost();

        _gsi.setExternalHost(gameExternalHost);
        _gsi.setInternalIp(gameInternalHost);

        if (!gameExternalHost.equals("*")) {
            try {
                _gsi.setExternalIp(InetAddress.getByName(gameExternalHost).getHostAddress());
            } catch (UnknownHostException e) {
                LOGGER.warn("Couldn't resolve hostname \"{}\"", gameExternalHost);
            }
        }
        else {
            _gsi.setExternalIp(_connectionIp);
        }

        if (!gameInternalHost.equals("*")) {
            try {
                _gsi.setInternalIp(InetAddress.getByName(gameInternalHost).getHostAddress());
            } catch (UnknownHostException e) {
                LOGGER.warn("Couldn't resolve hostname \"{}\"", gameInternalHost);
            }
        }
        else {
            _gsi.setInternalIp(_connectionIp);
        }

        LOGGER.info("Hooked gameserver: [{}] ", getServerId());
        LOGGER.info("Internal/External IP(s): {}/{}", (oldInternal == null) ? gameInternalHost : oldInternal, (oldExternal == null) ? gameExternalHost : oldExternal);
    }

    /**
     * @return the isAuthed.
     */
    public boolean isAuthed() {
        return (_gsi == null) ? false : _gsi.isAuthed();
    }

    public GameServerInfo getGameServerInfo() {
        return _gsi;
    }

    public void setGameServerInfo(GameServerInfo gsi) {
        _gsi = gsi;
    }

    /**
     * @return the connectionIpAddress.
     */
    public String getConnectionIpAddress() {
        return _connectionIPAddress;
    }

    /**
     * @return the server id.
     */
    private int getServerId() {
        return (_gsi == null) ? -1 : _gsi.getId();
    }
}