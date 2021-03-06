package net.sf.l2j.gameserver.model.location;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.database.AUserDefinedType;
import net.sf.l2j.commons.database.annotation.OrmTypeParam;

public class Location extends AUserDefinedType {

    @JacksonXmlProperty(localName = "X", isAttribute = true)
    @OrmTypeParam(0)
    protected volatile int posX;

    @JacksonXmlProperty(localName = "Y", isAttribute = true)
    @OrmTypeParam(1)
    protected volatile int posY;

    @JacksonXmlProperty(localName = "Z", isAttribute = true)
    @OrmTypeParam(2)
    protected volatile int posZ;

    @DefaultConstructor
    public Location() {}

    public Location(int x, int y, int z) {
        posX = x;
        posY = y;
        posZ = z;
    }

    @Override
    public String toString() {
        return "(" + posX + ", " + posY + ", " + posZ + ")";
    }

    @Override
    public int hashCode() {
        return posX ^ posY ^ posZ;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Location) {
            Location loc = (Location) o;
            return loc.posX == posX && loc.posY == posY && loc.posZ == posZ;
        }
        return false;
    }

    public boolean equals(int x, int y, int z) {
        return posX == x && posY == y && posZ == z;
    }

    public int getX() { return posX; }

    public int getY() { return posY; }

    public int getZ() { return posZ; }

    public void setXYZ(int x, int y, int z) {
        posX = x;
        posY = y;
        posZ = z;
    }
}