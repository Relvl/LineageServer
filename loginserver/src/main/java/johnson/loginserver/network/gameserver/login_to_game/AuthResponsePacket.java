package johnson.loginserver.network.gameserver.login_to_game;

import net.sf.l2j.network.ABaseSendablePacket;

public class AuthResponsePacket extends ABaseSendablePacket {
    public AuthResponsePacket(int serverId) {
        writeC(0x02);
        writeC(serverId);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}