package johnson.loginserver.network.client.client_to_login;

import johnson.loginserver.L2LoginClient;
import johnson.loginserver.LoginController;
import johnson.loginserver.LoginServer;
import johnson.loginserver.network.client.login_to_client.LoginFail.LoginFailReason;
import johnson.loginserver.network.client.login_to_client.PlayFail;
import johnson.loginserver.network.client.login_to_client.PlayOk;
import org.mmocore.network.ReceivablePacket;

public class RequestServer extends ReceivablePacket<L2LoginClient> {
    private int sKey1;
    private int sKey2;
    private int serverId;

    @Override
    public boolean read() {
        if (super._buf.remaining() >= 9) {
            sKey1 = readD();
            sKey2 = readD();
            serverId = readC();
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        if (!LoginServer.config.clientListener.showLicense || getClient().getSessionKey().checkLoginPair(sKey1, sKey2)) {

            if (LoginController.isLoginPossible(getClient(), serverId)) {
                getClient().setJoinedGS(true);
                getClient().sendPacket(new PlayOk(getClient().getSessionKey()));
            }
            else {
                getClient().close(PlayFail.PlayFailReason.REASON_TOO_MANY_PLAYERS);
            }
        }
        else {
            getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
        }
    }
}
