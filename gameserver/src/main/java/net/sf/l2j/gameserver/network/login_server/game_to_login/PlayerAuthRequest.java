package net.sf.l2j.gameserver.network.login_server.game_to_login;

import net.sf.l2j.gameserver.SessionKey;
import net.sf.l2j.network.ABaseSendablePacket;

public class PlayerAuthRequest extends ABaseSendablePacket {
    public PlayerAuthRequest(String login, SessionKey sessionKey) {
        writeC(0x05);
        writeS(login);
        writeD(sessionKey.playOkID1);
        writeD(sessionKey.playOkID2);
        writeD(sessionKey.loginOkID1);
        writeD(sessionKey.loginOkID2);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}