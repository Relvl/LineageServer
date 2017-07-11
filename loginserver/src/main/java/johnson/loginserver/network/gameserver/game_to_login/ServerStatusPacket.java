package johnson.loginserver.network.gameserver.game_to_login;

import johnson.loginserver.GameServerInfo;
import johnson.loginserver.GameServerTable;
import johnson.loginserver.network.gameserver.ABaseClientPacket;
import net.sf.l2j.commons.EServerStatus;

public class ServerStatusPacket extends ABaseClientPacket {

    public ServerStatusPacket(byte[] decrypt, int serverId) {
        super(decrypt);

        GameServerInfo gsi = GameServerTable.getInstance().getGameServer(serverId);
        if (gsi != null) {
            gsi.setShowingBrackets(readC() == 1);
            gsi.setShowingClock(readC() == 1);
            gsi.setTestServer(readC() == 1);
            gsi.setMaxPlayers(readD());
            gsi.setStatus(EServerStatus.getByCode(readD()));
        }
    }
}