package net.sf.l2j.gameserver.geoengine.geodata;

import net.sf.l2j.gameserver.geoengine.GeoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NullDriver extends GeoData {
    private static final Logger LOGGER = LoggerFactory.getLogger(NullDriver.class);

    public NullDriver() {
        LOGGER.info("NullDriver: Ready.");
    }

    @Override
    public int getGeoX(int worldX) {
        return worldX;
    }

    @Override
    public int getGeoY(int worldY) {
        return worldY;
    }

    @Override
    public int getWorldX(int geoX) {
        return geoX;
    }

    @Override
    public int getWorldY(int geoY) {
        return geoY;
    }

    @Override
    public boolean hasGeoPos(int geoX, int geoY) {
        return false;
    }

    @Override
    public short getHeightNearest(int geoX, int geoY, int worldZ) {
        return (short) worldZ;
    }

    @Override
    public final short getHeightAbove(int geoX, int geoY, int worldZ) {
        return (short) worldZ;
    }

    @Override
    public final short getHeightBelow(int geoX, int geoY, int worldZ) {
        return (short) worldZ;
    }

    @Override
    public byte getNsweNearest(int geoX, int geoY, int worldZ) {
        return 0x0F;
    }
}