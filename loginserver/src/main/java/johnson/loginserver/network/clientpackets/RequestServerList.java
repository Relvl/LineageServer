package johnson.loginserver.network.clientpackets;

import johnson.loginserver.network.ABaseLoginClientPacket;
import johnson.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import johnson.loginserver.network.serverpackets.ServerList;

public class RequestServerList extends ABaseLoginClientPacket {
    private int sKey1;
    private int sKey2;

    @Override
    public boolean readImpl() {
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
            getClient().sendPacket(new ServerList(getClient()));
        }
        else {
            getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
        }
    }
}