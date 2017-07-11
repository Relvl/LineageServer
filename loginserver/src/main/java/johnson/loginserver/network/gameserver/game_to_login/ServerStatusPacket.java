package johnson.loginserver.network.gameserver.game_to_login;

import net.sf.l2j.commons.EServerStatus;
import net.sf.l2j.network.ABaseReceivablePacket;

public class ServerStatusPacket extends ABaseReceivablePacket {
    private final boolean isBracket;
    private final boolean isClock;
    private final boolean isTestServer;
    private final int maxPlayers;
    private final EServerStatus serverStatus;

    public ServerStatusPacket(byte[] decrypt) {
        super(decrypt);
        isBracket = readC() == 1;
        isClock = readC() == 1;
        isTestServer = readC() == 1;
        maxPlayers = readD();
        serverStatus = EServerStatus.getByCode(readD());
    }

    public boolean isBracket() {
        return isBracket;
    }

    public boolean isClock() {
        return isClock;
    }

    public boolean isTestServer() {
        return isTestServer;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public EServerStatus getServerStatus() {
        return serverStatus;
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