package johnson.loginserver.network.gameserver.login_to_game;

import net.sf.l2j.commons.EGameServerLoginFailReason;
import net.sf.l2j.network.ABaseSendablePacket;

public class LoginFailPacket extends ABaseSendablePacket {
    public LoginFailPacket(EGameServerLoginFailReason reason) {
        writeC(0x01);
        writeC(reason.getCode());
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }

}
