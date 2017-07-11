package johnson.loginserver.network.gameserver.login_to_game;

import net.sf.l2j.network.ABaseSendablePacket;

public class KickPlayerPacket extends ABaseSendablePacket {
    public KickPlayerPacket(String login) {
        writeC(0x04);
        writeS(login);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}