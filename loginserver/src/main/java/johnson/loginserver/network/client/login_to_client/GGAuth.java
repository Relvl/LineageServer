package johnson.loginserver.network.client.login_to_client;

import johnson.loginserver.L2LoginClient;
import org.mmocore.network.SendablePacket;

public final class GGAuth extends SendablePacket<L2LoginClient> {
    private final int response;

    public GGAuth(int response) {
        this.response = response;
    }

    @Override
    protected void write() {
        writeC(0x0b);
        writeD(response);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
    }
}
