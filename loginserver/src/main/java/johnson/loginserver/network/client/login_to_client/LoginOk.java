package johnson.loginserver.network.client.login_to_client;

import johnson.loginserver.L2LoginClient;
import net.sf.l2j.commons.SessionKey;
import org.mmocore.network.SendablePacket;

public final class LoginOk extends SendablePacket<L2LoginClient> {
    private final int loginOk1;
    private final int loginOk2;

    public LoginOk(SessionKey sessionKey) {
        loginOk1 = sessionKey.loginOkID1;
        loginOk2 = sessionKey.loginOkID2;
    }

    @Override
    protected void write() {
        writeC(0x03);
        writeD(loginOk1);
        writeD(loginOk2);
        writeD(0x00);
        writeD(0x00);
        writeD(0x000003ea);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
        writeB(new byte[16]);
    }
}