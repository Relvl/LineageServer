package net.sf.l2j.gameserver.network.gameserverpackets;

public class AuthRequest extends GameServerBasePacket {
    /**
     * Format: cccSddb c desired ID c accept alternative ID c reserve Host s ExternalHostName s InetranlHostName d max players d hexid size b hexid
     */
    public AuthRequest(int id, boolean acceptAlternate, byte[] hexid, String externalHost, String internalHost, int port, boolean reserveHost, int maxplayer) {
        writeC(0x01);
        writeC(id);
        writeC(acceptAlternate ? 0x01 : 0x00);
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