package johnson.loginserver.network.client.client_to_login;

import johnson.loginserver.ELoginClientState;
import johnson.loginserver.L2LoginClient;
import johnson.loginserver.network.client.login_to_client.GGAuth;
import johnson.loginserver.network.client.login_to_client.LoginFail.LoginFailReason;
import org.mmocore.network.ReceivablePacket;

public class AuthGameGuard extends ReceivablePacket<L2LoginClient> {
    private int sessionId;

    @Override
    protected boolean read() {
        if (super._buf.remaining() >= 20) {
            sessionId = readD();
            // Это типа ключи для ГГ?
            readD();
            readD();
            readD();
            readD();

            return true;
        }
        return false;
    }

    @Override
    public void run() {
        if (sessionId == getClient().getSessionId()) {
            getClient().setState(ELoginClientState.AUTHED_GG);
            getClient().sendPacket(new GGAuth(getClient().getSessionId()));
        }
        else {
            getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
        }
    }
}