package net.sf.l2j.network.ls_gs_communication.impl.game_to_login;

import net.sf.l2j.commons.EServerStatus;
import net.sf.l2j.network.ls_gs_communication.AServerCommunicationPacket;

/**
 * @author Johnson / 12.07.2017
 */
public class GameServerStatusPacket extends AServerCommunicationPacket {
    private boolean isBracket;
    private boolean isClock;
    private boolean isTestServer;
    private int maxPlayers;
    private EServerStatus serverStatus;

    /** Чтение тела пакета. */
    @Override
    protected void doRead() {
        isBracket = readC() == 1;
        isClock = readC() == 1;
        isTestServer = readC() == 1;
        maxPlayers = readD();
        serverStatus = EServerStatus.getByCode(readD());
    }

    /** Запись тела пакета. */
    @Override
    protected void doWrite() {
        writeC(0x06);
        writeC(isBracket ? 1 : 0);
        writeC(isClock ? 1 : 0);
        writeC(isTestServer ? 1 : 0);
        writeD(maxPlayers);
        writeD(serverStatus.getCode());
    }

    public boolean isBracket() {
        return isBracket;
    }

    public void setIsBracket(boolean isBracket) {
        this.isBracket = isBracket;
    }

    public boolean isClock() {
        return isClock;
    }

    public void setIsClock(boolean isClock) {
        this.isClock = isClock;
    }

    public boolean isTestServer() {
        return isTestServer;
    }

    public void setIsTestServer(boolean isTestServer) {
        this.isTestServer = isTestServer;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public EServerStatus getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(EServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public String toString() {
        return "GameServerStatusPacket{" +
                "isBracket=" + isBracket +
                ", isClock=" + isClock +
                ", isTestServer=" + isTestServer +
                ", maxPlayers=" + maxPlayers +
                ", serverStatus=" + serverStatus +
                '}';
    }
}
