package net.sf.l2j.gameserver.network.login_server.game_to_login;

import net.sf.l2j.network.ABaseSendablePacket;

public class GameServerAuthRequestPacket extends ABaseSendablePacket {
    public GameServerAuthRequestPacket(int desiredId, byte[] hexid, String externalHost, String internalHost, int port, boolean reserveHost, int maxplayer) {
        writeC(0x01);
        writeC(desiredId);
        writeC(reserveHost ? 0x01 : 0x00);
        writeS(externalHost);
        writeS(internalHost);
        writeH(port);
        writeD(maxplayer);
        writeD(hexid.length);
        writeB(hexid);
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}