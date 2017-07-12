package net.sf.l2j.gameserver.network.client.game_to_client;

public class RadarControl extends L2GameServerPacket {
    private final int showRadar;
    private final int type;
    private final int posX;
    private final int posY;
    private final int posZ;

    public RadarControl(int showRadar, int type, int x, int y, int z) {
        this.showRadar = showRadar; // 0 = showradar; 1 = delete radar;
        this.type = type; // 1 - только стрелка над головой, 2 - флажок на карте
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    @Override
    protected void writeImpl() {
        writeC(0xEB);
        writeD(showRadar);
        writeD(type);
        writeD(posX);
        writeD(posY);
        writeD(posZ);
    }

    @Override
    public String toString() {
        return "RadarControl{" +
                "showRadar=" + showRadar +
                ", type=" + type +
                ", posX=" + posX +
                ", posY=" + posY +
                ", posZ=" + posZ +
                '}';
    }
}