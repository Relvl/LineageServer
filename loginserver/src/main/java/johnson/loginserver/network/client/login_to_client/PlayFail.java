package johnson.loginserver.network.client.login_to_client;

import johnson.loginserver.L2LoginClient;
import org.mmocore.network.SendablePacket;

public final class PlayFail extends SendablePacket<L2LoginClient> {
    private final PlayFailReason reason;

    public PlayFail(PlayFailReason reason) {
        this.reason = reason;
    }

    @Override
    protected void write() {
        writeC(0x06);
        writeC(reason.getCode());
    }

    @Override
    public String toString() {
        return "PlayFail{" +
                "reason=" + reason +
                '}';
    }

    public enum PlayFailReason {
        REASON_SYSTEM_ERROR(0x01),
        REASON_USER_OR_PASS_WRONG(0x02),
        REASON3(0x03),
        REASON4(0x04),
        REASON_TOO_MANY_PLAYERS(0x0F);

        private final int code;

        PlayFailReason(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}