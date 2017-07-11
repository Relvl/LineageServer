package net.sf.l2j.gameserver.network.login_server.game_to_login;

import net.sf.l2j.network.ABaseSendablePacket;

public class ChangeAccessLevelPacket extends ABaseSendablePacket {
    public ChangeAccessLevelPacket(String player, int access) {
        writeC(0x04);
        writeD(access);
        writeS(player);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}