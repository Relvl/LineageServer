package johnson.loginserver.network.gameserver.login_to_game;

import johnson.loginserver.LoginServer;
import johnson.loginserver.network.gameserver.ABaseServerPacket;

public class InitLS extends ABaseServerPacket {

    public InitLS(byte[] publickey) {
        writeC(0x00);
        writeD(LoginServer.config.protocolRevision);
        writeD(publickey.length);
        writeB(publickey);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}
