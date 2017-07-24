package net.sf.l2j.gameserver.model.location;

public class L2TeleportLocation extends Location {
    private int teleportId;
    private int price;
    private boolean forNoble;

    public void setTeleId(int id) { teleportId = id; }

    public int getTeleId() { return teleportId; }

    public void setPrice(int price) { this.price = price; }

    public int getPrice() { return price; }

    public void setIsForNoble(boolean val) { forNoble = val; }

    public boolean isForNoble() { return forNoble; }
}