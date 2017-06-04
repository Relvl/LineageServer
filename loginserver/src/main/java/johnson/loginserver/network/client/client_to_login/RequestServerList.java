package johnson.loginserver.network.client.client_to_login;

import johnson.loginserver.L2LoginClient;
import johnson.loginserver.network.client.login_to_client.List;
import johnson.loginserver.network.client.login_to_client.LoginFail.LoginFailReason;
import org.mmocore.network.ReceivablePacket;

public class RequestServerList extends ReceivablePacket<L2LoginClient> {
    private int sKey1;
    private int sKey2;

    @Override
    public boolean read() {
        if (super._buf.remaining() >= 8) {
            sKey1 = readD(); // loginOk 1
            sKey2 = readD(); // loginOk 2
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        if (getClient().getSessionKey().checkLoginPair(sKey1, sKey2)) {
            getClient().sendPacket(new List(getClient()));
        }
        else {
            getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
        }
    }
}