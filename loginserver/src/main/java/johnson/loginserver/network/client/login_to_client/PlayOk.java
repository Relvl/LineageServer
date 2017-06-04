package johnson.loginserver.network.client.login_to_client;

import johnson.loginserver.L2LoginClient;
import johnson.loginserver.SessionKey;
import org.mmocore.network.SendablePacket;

public final class PlayOk extends SendablePacket<L2LoginClient> {
    private final int playOk1;
    private final int playOk2;

    public PlayOk(SessionKey sessionKey) {
        playOk1 = sessionKey.playOkID1;
        playOk2 = sessionKey.playOkID2;
    }

    @Override
    protected void write() {
        writeC(0x07);
        writeD(playOk1);
        writeD(playOk2);
    }
}