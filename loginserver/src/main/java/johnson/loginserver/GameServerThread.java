package johnson.loginserver;

import johnson.loginserver.network.gameserver.ABaseServerPacket;
import johnson.loginserver.network.gameserver.game_to_login.*;
import johnson.loginserver.network.gameserver.login_to_game.*;
import johnson.loginserver.security.SecurityController;
import net.sf.l2j.L2DatabaseFactory;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class GameServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServerThread.class);

    private final Socket socket;
    private final String serverIp;

    private final RSAPublicKey _publicKey;
    private final RSAPrivateKey _privateKey;
    private final Collection<String> _accountsOnGameServer = new HashSet<>();

    private InputStream _in;
    private OutputStream _out;
    private NewCrypt _blowfish;
    private GameServerInfo gameServerInfo;
    private String _connectionIPAddress;

    public GameServerThread(Socket socket) {
        this.socket = socket;
        this.serverIp = socket.getInetAddress().getHostAddress();

        try {
            _in = this.socket.getInputStream();
            _out = new BufferedOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        KeyPair pair = GameServerTable.getInstance().getRandomKeyPair();
        _privateKey = (RSAPrivateKey) pair.getPrivate();
        _publicKey = (RSAPublicKey) pair.getPublic();
        _blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");

        start();
    }

    public static boolean isBannedGameserverIP(String ipAddress) {
        InetAddress netAddress = null;
        try {
            netAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            LOGGER.error("", e);
        }
        return SecurityController.getInstance().isBannedAddress(netAddress);
    }

    @Override
    public void run() {
        _connectionIPAddress = socket.getInetAddress().getHostAddress();

        // Ensure no further processing for this connection if server is considered as banned.
        if (isBannedGameserverIP(_connectionIPAddress)) {
            LOGGER.info("GameServer with banned IP {} tries to register.", _connectionIPAddress);
            forceClose(LoginAFailServerPacket.REASON_IP_BANNED);
            return;
        }

        try {
            sendPacket(new InitLSPacket(_publicKey.getModulus().toByteArray()));

            while (true) {
                int lengthLo = _in.read();
                int lengthHi = _in.read();
                int length = lengthHi * 256 + lengthLo;

                if (lengthHi < 0 || socket.isClosed()) {
                    break;
                }

                byte[] data = new byte[length - 2];

                int receivedBytes = 0;
                int newBytes = 0;
                while (newBytes != -1 && receivedBytes < length - 2) {
                    newBytes = _in.read(data, 0, length - 2);
                    receivedBytes += newBytes;
                }

                if (receivedBytes != length - 2) {
                    LOGGER.warn("Incomplete packet is sent to the server, closing connection.");
                    break;
                }

                // decrypt if we have a key
                data = _blowfish.decrypt(data);
                if (!NewCrypt.verifyChecksum(data)) {
                    LOGGER.warn("Incorrect packet checksum, closing connection.");
                    return;
                }

                int packetType = data[0] & 0xff;
                switch (packetType) {
                    case 0x00:
                        onReceiveBlowfishKey(data);
                        break;

                    case 0x01:
                        onGameServerAuth(data);
                        break;

                    case 0x02:
                        onReceivePlayerInGame(data);
                        break;

                    case 0x03:
                        onReceivePlayerLogOut(data);
                        break;

                    case 0x04:
                        onReceiveChangeAccessLevel(data);
                        break;

                    case 0x05:
                        onReceivePlayerAuthRequest(data);
                        break;

                    case 0x06:
                        onReceiveServerStatus(data);
                        break;

                    default:
                        LOGGER.warn("Unknown Opcode (0x{}) from GameServer, closing connection.", Integer.toHexString(packetType).toUpperCase());
                        forceClose(LoginAFailServerPacket.NOT_AUTHED);
                }

            }
        } catch (IOException e) {
            String serverName = getServerId() == -1 ? '(' + _connectionIPAddress + ')' : "[" + getServerId() + "] ";
            LOGGER.info("GameServer {}: {}.", serverName, e.getMessage());
        } finally {
            if (isAuthed()) {
                gameServerInfo.setDown();
                LOGGER.info("GameServer [{}]  is now set as disconnected.", getServerId());
            }
            LoginServer.getInstance().getGameServerListener().removeGameServer(this);
            LoginServer.getInstance().getGameServerListener().removeFloodProtection(serverIp);
        }
    }

    private void onReceiveBlowfishKey(byte[] data) {
        BlowFishKeyPacket packet = new BlowFishKeyPacket(data, _privateKey);
        _blowfish = new NewCrypt(packet.getKey());
    }

    private void onGameServerAuth(byte[] data) {
        GameServerAuthRequestPacket packet = new GameServerAuthRequestPacket(data);
        GameServerInfo gsi = GameServerTable.getInstance().getGameServer(packet.getDesiredID());

        if (gsi != null) {
            if (Arrays.equals(gsi.getHexId(), packet.getHexID())) {
                // check to see if this GS is already connected
                synchronized (gsi) {
                    if (gsi.isAuthed()) {
                        forceClose(LoginAFailServerPacket.REASON_ALREADY_LOGGED_IN);
                    }
                    else {
                        attachGameServerInfo(gsi, packet);
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

        if (isAuthed()) {
            sendPacket(new AuthResponsePacket(gameServerInfo.getId()));
        }
    }

    private void onReceivePlayerInGame(byte[] data) {
        if (!isAuthed()) {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
            return;
        }
        PlayerInGamePacket packet = new PlayerInGamePacket(data);
        for (String account : packet.getAccounts()) {
            _accountsOnGameServer.add(account);
        }
    }

    private void onReceivePlayerLogOut(byte[] data) {
        if (!isAuthed()) {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
            return;
        }
        PlayerLogoutPacket packet = new PlayerLogoutPacket(data);
        _accountsOnGameServer.remove(packet.getAccount());
    }

    private void onReceiveChangeAccessLevel(byte[] data) {
        if (!isAuthed()) {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
            return;
        }
        ChangeAccessLevelPacket packet = new ChangeAccessLevelPacket(data);

        // FIXME@SQL gameservers
        try (
                Connection con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement statement = con.prepareStatement("UPDATE accounts SET access_level=? WHERE login=?")
        ) {
            statement.setInt(1, packet.getLevel());
            statement.setString(2, packet.getAccount());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            LOGGER.warn("Could not set accessLevel: {}", e.getMessage(), e);
        }
        LOGGER.info("Changed {} access level to {}.", packet.getAccount(), packet.getLevel());
    }

    private void onReceivePlayerAuthRequest(byte[] data) {
        if (!isAuthed()) {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
            return;
        }
        PlayerAuthRequestPacket packet = new PlayerAuthRequestPacket(data);
        SessionKey key = LoginController.getInstance().getKeyForAccount(packet.getAccount());

        if (key != null && key.equals(packet.getKey())) {
            LoginController.getInstance().removeClient(packet.getAccount());
            sendPacket(new PlayerAuthResponsePacket(packet.getAccount(), true));
        }
        else {
            sendPacket(new PlayerAuthResponsePacket(packet.getAccount(), false));
        }
    }

    private void onReceiveServerStatus(byte[] data) {
        if (!isAuthed()) {
            forceClose(LoginAFailServerPacket.NOT_AUTHED);
            return;
        }
        ServerStatusPacket packet = new ServerStatusPacket(data, getServerId());
    }

    public boolean hasAccountOnGameServer(String account) {
        return _accountsOnGameServer.contains(account);
    }

    public int getPlayerCount() {
        return _accountsOnGameServer.size();
    }

    private void attachGameServerInfo(GameServerInfo gsi, GameServerAuthRequestPacket gameServerAuthRequestPacket) {
        gameServerInfo = gsi;
        gameServerInfo.setGameServerThread(this);
        gameServerInfo.setPort(gameServerAuthRequestPacket.getPort());
        setGameHosts(gameServerAuthRequestPacket.getExternalHost(), gameServerAuthRequestPacket.getInternalHost());
        gameServerInfo.setMaxPlayers(gameServerAuthRequestPacket.getMaxPlayers());
        gameServerInfo.setAuthed(true);
    }

    private void forceClose(int reason) {
        sendPacket(new LoginAFailServerPacket(reason));
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.warn("GameServerThread: Failed disconnecting banned server, server already disconnected.", e);
        }
    }

    private void sendPacket(ABaseServerPacket serverPacket) {
        try {
            byte[] data = serverPacket.getContent();
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
            LOGGER.error("IOException while sending packet {}.", serverPacket.getClass().getSimpleName());
        }
    }

    public void kickPlayer(String account) {
        sendPacket(new KickPlayerPacket(account));
    }

    public void setGameHosts(String gameExternalHost, String gameInternalHost) {
        String oldInternal = gameServerInfo.getInternalHost();
        String oldExternal = gameServerInfo.getExternalHost();

        gameServerInfo.setExternalHost(gameExternalHost);
        gameServerInfo.setInternalIp(gameInternalHost);

        if ("*".equals(gameExternalHost)) {
            gameServerInfo.setExternalIp(serverIp);
        }
        else {
            try {
                gameServerInfo.setExternalIp(InetAddress.getByName(gameExternalHost).getHostAddress());
            } catch (UnknownHostException ignored) {
                LOGGER.warn("Couldn't resolve hostname \"{}\"", gameExternalHost);
            }
        }

        if ("*".equals(gameInternalHost)) {
            gameServerInfo.setInternalIp(serverIp);
        }
        else {
            try {
                gameServerInfo.setInternalIp(InetAddress.getByName(gameInternalHost).getHostAddress());
            } catch (UnknownHostException ignored) {
                LOGGER.warn("Couldn't resolve hostname \"{}\"", gameInternalHost);
            }
        }

        LOGGER.info("Hooked gameserver: [{}] ", getServerId());
        LOGGER.info("Internal/External IP(s): {}/{}", oldInternal == null ? gameInternalHost : oldInternal, oldExternal == null ? gameExternalHost : oldExternal);
    }

    public boolean isAuthed() {
        return gameServerInfo != null && gameServerInfo.isAuthed();
    }

    private int getServerId() {
        return gameServerInfo == null ? -1 : gameServerInfo.getId();
    }
}