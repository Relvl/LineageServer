package johnson.loginserver.network.gameserver.login_to_game;

import net.sf.l2j.network.ABaseSendablePacket;

public class PlayerAuthResponsePacket extends ABaseSendablePacket {

    public PlayerAuthResponsePacket(String account, boolean authed) {
        writeC(0x03);
        writeS(account);
        writeC(authed ? 1 : 0);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}