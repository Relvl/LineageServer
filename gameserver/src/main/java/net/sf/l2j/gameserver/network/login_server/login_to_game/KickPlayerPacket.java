package net.sf.l2j.gameserver.network.login_server.login_to_game;

import net.sf.l2j.network.ABaseReceivablePacket;

public class KickPlayerPacket extends ABaseReceivablePacket {

    private final String login;

    public KickPlayerPacket(byte[] decrypt) {
        super(decrypt);
        login = readS();
    }

    public String getAccount() {
        return login;
    }

}