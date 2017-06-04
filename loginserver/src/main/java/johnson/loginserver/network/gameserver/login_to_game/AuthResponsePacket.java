package johnson.loginserver.network.gameserver.login_to_game;

import johnson.loginserver.network.gameserver.ABaseServerPacket;

public class AuthResponsePacket extends ABaseServerPacket {
    public AuthResponsePacket(int serverId) {
        writeC(0x02);
        writeC(serverId);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}