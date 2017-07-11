package johnson.loginserver;

import johnson.loginserver.network.gameserver.game_to_login.ServerStatusPacket;
import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.EServerStatus;
import net.sf.l2j.commons.database.annotation.OrmParamCursor;

/**
 * @author Johnson / 02.06.2017
 */
public class GameServerInfo {
    @OrmParamCursor("ID")
    private final int id;
    @OrmParamCursor("HEXID")
    private final byte[] hexId;

    private GameServerThread gst;
    private EServerStatus status;
    private boolean isAuthed;
    private String internalIp;
    private String externalIp;
    private String externalHost;
    private int port;
    private boolean isTestServer;
    private boolean isShowingClock;
    private boolean isShowingBrackets;
    private int maxPlayers;

    @DefaultConstructor
    public GameServerInfo() {
        this.id = -1;
        this.hexId = null;
        this.gst = null;
        this.status = EServerStatus.STATUS_DOWN;
    }

    public int getId() {
        return id;
    }

    public byte[] getHexId() {
        return hexId;
    }

    public boolean isAuthed() {
        return isAuthed;
    }

    public void setAuthed(boolean isAuthed) {
        this.isAuthed = isAuthed;
    }

    public GameServerThread getGameServerThread() {
        return gst;
    }

    public void setGameServerThread(GameServerThread gst) {
        this.gst = gst;
    }

    public EServerStatus getStatus() {
        return status;
    }

    public void setStatus(EServerStatus status) {
        System.out.println(">>> status: " + status);
        this.status = status;
    }

    public int getCurrentPlayerCount() {
        if (gst == null) {
            return 0;
        }
        return gst.getPlayerCount();
    }

    public void setInternalIp(String internalIp) {
        this.internalIp = internalIp;
    }

    public String getInternalHost() {
        return internalIp;
    }

    public String getExternalIp() {
        return externalIp;
    }

    public void setExternalIp(String externalIp) {
        this.externalIp = externalIp;
    }

    public String getExternalHost() {
        return externalHost;
    }

    public void setExternalHost(String externalHost) {
        this.externalHost = externalHost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public boolean isPvp() {
        return true;
    }

    public boolean isTestServer() {
        return isTestServer;
    }

    public void setTestServer(boolean val) {
        isTestServer = val;
    }

    public boolean isShowingClock() {
        return isShowingClock;
    }

    public void setShowingClock(boolean clock) {
        isShowingClock = clock;
    }

    public boolean isShowingBrackets() {
        return isShowingBrackets;
    }

    public void setShowingBrackets(boolean val) {
        isShowingBrackets = val;
    }

    public void setDown() {
        setAuthed(false);
        setPort(0);
        setGameServerThread(null);
        setStatus(EServerStatus.STATUS_DOWN);
    }

    @Override
    public String toString() {
        return "GameServerInfo{" + id + ": hexId=[" + new String(hexId) + "]}";
    }
}
