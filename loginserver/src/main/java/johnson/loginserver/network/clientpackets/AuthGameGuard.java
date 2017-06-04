package johnson.loginserver.network.clientpackets;

import johnson.loginserver.ELoginClientState;
import johnson.loginserver.network.ABaseLoginClientPacket;
import johnson.loginserver.network.serverpackets.GGAuth;
import johnson.loginserver.network.serverpackets.LoginFail.LoginFailReason;

public class AuthGameGuard extends ABaseLoginClientPacket {
    private int sessionId;

    @Override
    protected boolean readImpl() {
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