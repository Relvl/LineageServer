package net.sf.l2j.gameserver.network.login_server.login_to_game;

import net.sf.l2j.network.ABaseReceivablePacket;

public class PlayerAuthResponsePacket extends ABaseReceivablePacket {
    private final String account;
    private final boolean authed;

    public PlayerAuthResponsePacket(byte[] decrypt) {
        super(decrypt);

        account = readS();
        authed = readC() != 0;
    }

    public String getAccount() {
        return account;
    }

    public boolean isAuthed() {
        return authed;
    }


}