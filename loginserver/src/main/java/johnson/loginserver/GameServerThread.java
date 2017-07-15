package johnson.loginserver;

import johnson.loginserver.security.SecurityController;
import net.sf.l2j.NewCrypt;
import net.sf.l2j.commons.EGameServerLoginFailReason;
import net.sf.l2j.commons.SessionKey;
import net.sf.l2j.network.ls_gs_communication.AServerCommunicationThread;
import net.sf.l2j.network.ls_gs_communication.impl.game_to_login.*;
import net.sf.l2j.network.ls_gs_communication.impl.login_to_game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class GameServerThread extends AServerCommunicationThread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServerThread.class);

    private final Socket socket;
    private final String serverIp;
    private final RSAPrivateKey privateKey;
    private final Collection<String> accountsOnGameServer = new HashSet<>();

    private GameServerInfo gameServerInfo;

    public GameServerThread(Socket socket) {
        super("GameServerThread");

        this.socket = socket;
        this.serverIp = socket.getInetAddress().getHostAddress();

        try {
            inputStream = this.socket.getInputStream();
            outputStream = new BufferedOutputStream(this.socket.getOutputStream());
        }
        catch (IOException e) {
            LOGGER.error("", e);
        }

        KeyPair pair = GameServerTable.getInstance().getRandomKeyPair();
        privateKey = (RSAPrivateKey) pair.getPrivate();

        publicKey = (RSAPublicKey) pair.getPublic();
        blowfishCrypt = new NewCrypt(BLOWFISH_KEY_BASE);
    }

    public static boolean isBannedGameserverIP(String ipAddress) {
        InetAddress netAddress = null;
        try {
            netAddress = InetAddress.getByName(ipAddress);
        }
        catch (UnknownHostException e) {
            LOGGER.error("", e);
        }
        return SecurityController.getInstance().isBannedAddress(netAddress);
    }

    @Override
    public void run() {
        String connectionIPAddress = socket.getInetAddress().getHostAddress();

        // Ensure no further processing for this connection if server is considered as banned.
        if (isBannedGameserverIP(connectionIPAddress)) {
            LOGGER.info("GameServer with banned IP {} tries to register.", connectionIPAddress);
            forceClose(EGameServerLoginFailReason.REASON_IP_BANNED);
            return;
        }

        try {
            sendPacket(new InitGameServerPacket(LoginServer.CONFIG.network.communicationProtocol, publicKey.getModulus().toByteArray()));
            doThreadLoop(socket);
        }
        catch (IOException e) {
            String serverName = getServerId() == -1 ? '(' + connectionIPAddress + ')' : "[" + getServerId() + "] ";
            LOGGER.info("GameServer {}: {}.", serverName, e.getMessage());
        }
        finally {
            if (isAuthed()) {
                gameServerInfo.setDown();
                LOGGER.info("GameServer [{}]  is now set as disconnected.", getServerId());
            }
            GameServerListener.removeGameServer(this);
            LoginServer.getInstance().getGameServerListener().removeFloodProtection(serverIp);
        }
    }

    @Override
    protected void doProcessIncomindData(int packetType, byte[] incoming) {
        switch (packetType) {
            case 0x00:
                onReceiveBlowfishKey(incoming);
                break;
            case 0x01:
                onGameServerAuth(incoming);
                break;
            case 0x02:
                onReceivePlayerInGame(incoming);
                break;
            case 0x03:
                onReceivePlayerLogOut(incoming);
                break;
            case 0x04:
                // access level
                break;
            case 0x05:
                onReceivePlayerAuthRequest(incoming);
                break;
            case 0x06:
                onReceiveServerStatus(incoming);
                break;
            default:
                LOGGER.warn("Unknown Opcode (0x{}) from GameServer, closing connection.", Integer.toHexString(packetType).toUpperCase());
                forceClose(EGameServerLoginFailReason.REASON_NOT_AUTHED);
        }
    }

    private void onReceiveBlowfishKey(byte[] data) {
        BlowFishKeyInitPacket packet = new BlowFishKeyInitPacket(data, privateKey);
        blowfishCrypt = new NewCrypt(packet.getBlowFishKey());
    }

    private void onGameServerAuth(byte[] data) {
        GameServerAuthRequestPacket packet = new GameServerAuthRequestPacket(data);
        GameServerInfo gsi = GameServerTable.getInstance().getGameServer(packet.getDesiredId());

        if (gsi == null) {
            forceClose(EGameServerLoginFailReason.REASON_WRONG_HEXID);
            return;
        }

        if (!Arrays.equals(gsi.getHexId(), packet.getHexId())) {
            forceClose(EGameServerLoginFailReason.REASON_WRONG_HEXID);
            return;
        }

        // TODO Нах тут синхро? Разобраться и либо переделать на файнал лок, либо снести.
        synchronized (gsi) {
            if (gsi.isAuthed()) {
                forceClose(EGameServerLoginFailReason.REASON_ALREADY_LOGGED_IN);
            }
            else {
                gameServerInfo = gsi;
                gameServerInfo.setGameServerThread(this);
                gameServerInfo.setPort(packet.getPort());
                gameServerInfo.setMaxPlayers(packet.getMaxPlayers());
                gameServerInfo.setAuthed(true);
                setGameHosts(packet.getExternalHost(), packet.getInternalHost());
            }
        }

        if (isAuthed()) {
            sendPacket(new LoginServerAuthResponsePacket(gameServerInfo.getId()));
        }
    }

    private void onReceivePlayerInGame(byte[] data) {
        if (!isAuthed()) {
            forceClose(EGameServerLoginFailReason.REASON_NOT_AUTHED);
            return;
        }
        PlayerInGameServerPacket packet = new PlayerInGameServerPacket(data);
        for (String account : packet.getLogins()) {
            accountsOnGameServer.add(account);
        }
    }

    private void onReceivePlayerLogOut(byte[] data) {
        if (!isAuthed()) {
            forceClose(EGameServerLoginFailReason.REASON_NOT_AUTHED);
            return;
        }
        PlayerLogoutFromGamePacket packet = new PlayerLogoutFromGamePacket(data);
        accountsOnGameServer.remove(packet.getLogin());
    }

    private void onReceivePlayerAuthRequest(byte[] data) {
        if (!isAuthed()) {
            forceClose(EGameServerLoginFailReason.REASON_NOT_AUTHED);
            return;
        }
        PlayerAuthRequestPacket packet = new PlayerAuthRequestPacket(data);
        SessionKey key = LoginController.getInstance().getKeyForAccount(packet.getLogin());

        if (key != null && key.equals(packet.getSessionKey())) {
            LoginController.getInstance().removeClient(packet.getLogin());
            sendPacket(new PlayerAuthResponsePacket(packet.getLogin(), true));
        }
        else {
            sendPacket(new PlayerAuthResponsePacket(packet.getLogin(), false));
        }
    }

    private void onReceiveServerStatus(byte[] data) {
        if (!isAuthed()) {
            forceClose(EGameServerLoginFailReason.REASON_NOT_AUTHED);
            return;
        }
        GameServerStatusPacket packet = new GameServerStatusPacket(data);
        GameServerInfo gsi = GameServerTable.getInstance().getGameServer(getServerId());
        if (gsi != null) {
            gsi.setShowingBrackets(packet.isBracket());
            gsi.setShowingClock(packet.isClock());
            gsi.setTestServer(packet.isTestServer());
            gsi.setMaxPlayers(packet.getMaxPlayers());
            gsi.setStatus(packet.getServerStatus());
        }
    }

    public boolean hasAccountOnGameServer(String account) {
        return accountsOnGameServer.contains(account);
    }

    public int getPlayerCount() {
        return accountsOnGameServer.size();
    }

    private void forceClose(EGameServerLoginFailReason reason) {
        sendPacket(new GameServerLoginFailPacket(reason));
        try {
            socket.close();
        }
        catch (IOException e) {
            LOGGER.warn("GameServerThread: Failed disconnecting banned server, server already disconnected.", e);
        }
    }

    /** По сути чисто ради того, чтобы выкинуть старый клиент при повторном логине. */
    public void kickPlayer(String login) {
        sendPacket(new KickPlayerFromGamePacket(login));
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
            }
            catch (UnknownHostException ignored) {
                LOGGER.warn("Couldn't resolve hostname \"{}\"", gameExternalHost);
            }
        }

        if ("*".equals(gameInternalHost)) {
            gameServerInfo.setInternalIp(serverIp);
        }
        else {
            try {
                gameServerInfo.setInternalIp(InetAddress.getByName(gameInternalHost).getHostAddress());
            }
            catch (UnknownHostException ignored) {
                LOGGER.warn("Couldn't resolve hostname \"{}\"", gameInternalHost);
            }
        }

        LOGGER.info("Hooked gameserver: [{}] ", getServerId());
        LOGGER.info("Internal/External IP(s): {}/{}", oldInternal == null ? gameInternalHost : oldInternal, oldExternal == null ? gameExternalHost : oldExternal);
    }

    private boolean isAuthed() {
        return gameServerInfo != null && gameServerInfo.isAuthed();
    }

    private int getServerId() {
        return gameServerInfo == null ? -1 : gameServerInfo.getId();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String toString() {
        return "GameServerThread{" +
                "socket=" + socket +
                ", serverIp='" + serverIp + '\'' +
                ", privateKey=" + privateKey +
                ", accountsOnGameServer=" + accountsOnGameServer +
                ", gameServerInfo=" + gameServerInfo +
                '}';
    }
}