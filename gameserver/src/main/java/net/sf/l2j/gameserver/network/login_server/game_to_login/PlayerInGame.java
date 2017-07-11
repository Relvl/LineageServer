package net.sf.l2j.gameserver.network.login_server.game_to_login;

import net.sf.l2j.network.ABaseSendablePacket;

import java.util.List;

public class PlayerInGame extends ABaseSendablePacket {

    public PlayerInGame(String login) {
        writeC(0x02);
        writeH(1);
        writeS(login);
    }

    public PlayerInGame(List<String> logins) {
        writeC(0x02);
        writeH(logins.size());
        for (String pc : logins) { writeS(pc); }
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}