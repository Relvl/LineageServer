package johnson.loginserver.network.client.login_to_client;

import johnson.loginserver.L2LoginClient;
import net.sf.l2j.commons.SessionKey;
import org.mmocore.network.SendablePacket;

public final class PlayOk extends SendablePacket<L2LoginClient> {
    private final int playOk1;
    private final int playOk2;

    public PlayOk(SessionKey sessionKey) {
        playOk1 = sessionKey.getPlayOkID1();
        playOk2 = sessionKey.getPlayOkID2();
    }

    @Override
    protected void write() {
        writeC(0x07);
        writeD(playOk1);
        writeD(playOk2);
    }
}