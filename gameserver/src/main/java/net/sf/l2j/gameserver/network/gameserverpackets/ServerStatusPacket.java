package net.sf.l2j.gameserver.network.gameserverpackets;

import net.sf.l2j.commons.EServerStatus;

public class ServerStatusPacket extends GameServerBasePacket {
    private boolean isBracket;
    private boolean isClock;
    private boolean isTestServer;
    private int maxPlayers;
    private EServerStatus serverStatus;

    public void setIsBracket(boolean isBracket) {
        this.isBracket = isBracket;
    }

    public void setIsClock(boolean isClock) {
        this.isClock = isClock;
    }

    public void setIsTestServer(boolean isTestServer) {
        this.isTestServer = isTestServer;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setServerStatus(EServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public byte[] getContent() {
        writeC(0x06);

        writeC(isBracket ? 1 : 0);
        writeC(isClock ? 1 : 0);
        writeC(isTestServer ? 1 : 0);
        writeD(maxPlayers);
        writeD(serverStatus.getCode());

        return getBytes();
    }

    @Override
    public String toString() {
        return "ServerStatusPacket{" +
                "isBracket=" + isBracket +
                ", isClock=" + isClock +
                ", isTestServer=" + isTestServer +
                ", maxPlayers=" + maxPlayers +
                ", serverStatus=" + serverStatus +
                '}';
    }
}