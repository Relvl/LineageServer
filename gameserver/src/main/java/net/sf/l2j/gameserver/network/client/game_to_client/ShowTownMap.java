package net.sf.l2j.gameserver.network.client.game_to_client;

public class ShowTownMap extends L2GameServerPacket {
    private final String texture;
    private final int x;
    private final int y;

    public ShowTownMap(String texture, int x, int y) {
        this.texture = texture;
        this.x = x;
        this.y = y;
    }

    @Override
    protected final void writeImpl() {
        writeC(0xde);
        writeS(texture);
        writeD(x);
        writeD(y);
    }
}