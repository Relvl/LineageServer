package johnson.loginserver.network.loginserverpackets;

import johnson.loginserver.LoginServer;
import johnson.loginserver.network.serverpackets.ServerBasePacket;

/**  */
public class InitLS extends ServerBasePacket {
    // ID 0x00
    // format
    // d proto rev
    // d key size
    // b key

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
