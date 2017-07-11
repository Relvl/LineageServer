package net.sf.l2j.gameserver.network.login_server.login_to_game;

import net.sf.l2j.network.ABaseReceivablePacket;

public class AuthResponsePacket extends ABaseReceivablePacket {

    private final int serverId;

    public AuthResponsePacket(byte[] decrypt) {
        super(decrypt);
        serverId = readC();
    }

    public int getServerId() {
        return serverId;
    }

}
