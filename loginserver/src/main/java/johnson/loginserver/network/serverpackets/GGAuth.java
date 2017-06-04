package johnson.loginserver.network.serverpackets;

import johnson.loginserver.network.ABaseLoginServerPacket;

public final class GGAuth extends ABaseLoginServerPacket {
    private final int _response;

    public GGAuth(int response) {
        _response = response;
    }

    @Override
    protected void write() {
        writeC(0x0b);
        writeD(_response);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
    }
}
