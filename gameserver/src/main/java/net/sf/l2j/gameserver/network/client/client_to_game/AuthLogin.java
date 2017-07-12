package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.commons.SessionKey;
import net.sf.l2j.gameserver.LoginServerThread;

public final class AuthLogin extends L2GameClientPacket {
    private String loginName;
    private int playKey1;
    private int playKey2;
    private int loginKey1;
    private int loginKey2;

    @Override
    protected void readImpl() {
        loginName = readS().toLowerCase();
        playKey2 = readD();
        playKey1 = readD();
        loginKey1 = readD();
        loginKey2 = readD();
    }

    @Override
    protected void runImpl() {
        if (getClient().getAccountName() == null) {
            if (LoginServerThread.getInstance().addGameServerLogin(loginName, getClient())) {
                getClient().setAccountName(loginName);
                LoginServerThread.getInstance().addWaitingClientAndSendRequest(loginName, getClient(), new SessionKey(loginKey1, loginKey2, playKey1, playKey2));
            }
            else { getClient().close(null); }
        }
    }

    @Override
    public String toString() {
        return "AuthLogin{" +
                "loginName='" + loginName + '\'' +
                ", playKey1=" + playKey1 +
                ", playKey2=" + playKey2 +
                ", loginKey1=" + loginKey1 +
                ", loginKey2=" + loginKey2 +
                '}';
    }
}