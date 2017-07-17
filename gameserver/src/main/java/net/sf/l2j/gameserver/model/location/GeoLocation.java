package net.sf.l2j.gameserver.model.location;

import net.sf.l2j.gameserver.geoengine.GeoData;

public class GeoLocation extends Location {
    private byte nswe;

    public GeoLocation(int x, int y, int z) {
        super(x, y, GeoData.getInstance().getHeightNearest(x, y, z));
        nswe = GeoData.getInstance().getNsweNearest(x, y, z);
    }

    public void set(int x, int y, short z) {
        setXYZ(x, y, GeoData.getInstance().getHeightNearest(x, y, z));
        nswe = GeoData.getInstance().getNsweNearest(x, y, z);
    }

    public int getGeoX() {
        return posX;
    }

    public int getGeoY() {
        return posY;
    }

    @Override
    public int getX() {
        return GeoData.getInstance().getWorldX(posX);
    }

    @Override
    public int getY() {
        return GeoData.getInstance().getWorldY(posY);
    }

    public byte getNSWE() {
        return nswe;
    }
}