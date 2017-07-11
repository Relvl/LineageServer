package net.sf.l2j.gameserver.network.login_server.game_to_login;

import net.sf.l2j.network.ABaseSendablePacket;

public class PlayerLogout extends ABaseSendablePacket {
    public PlayerLogout(String login) {
        writeC(0x03);
        writeS(login);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}