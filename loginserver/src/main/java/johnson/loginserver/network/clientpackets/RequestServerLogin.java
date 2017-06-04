package johnson.loginserver.network.clientpackets;

import johnson.loginserver.LoginController;
import johnson.loginserver.LoginServer;
import johnson.loginserver.network.ABaseLoginClientPacket;
import johnson.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import johnson.loginserver.network.serverpackets.PlayFail;
import johnson.loginserver.network.serverpackets.PlayOk;

public class RequestServerLogin extends ABaseLoginClientPacket {
    private int sKey1;
    private int sKey2;
    private int serverId;

    @Override
    public boolean readImpl() {
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

            if (LoginController.getInstance().isLoginPossible(getClient(), serverId)) {
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
