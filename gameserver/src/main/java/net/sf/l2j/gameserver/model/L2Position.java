package net.sf.l2j.gameserver.model;

public final class L2Position {
    public final int posX;
    public final int posY;
    public final int posZ;
    public final int heading;

    public L2Position(int posX, int posY, int posZ, int heading) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.heading = heading;
    }

    @Override
    public String toString() {
        return "L2Position{" +
                "posX=" + posX +
                ", posY=" + posY +
                ", posZ=" + posZ +
                ", heading=" + heading +
                '}';
    }
}