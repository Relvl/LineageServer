package net.sf.l2j.gameserver.model;

public final class SoulCrystalAbsorbInfo {
    private boolean registered;
    private int itemId;
    private int absorbedHpPercent;

    public SoulCrystalAbsorbInfo(int itemId) {
        this.itemId = itemId;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean state) {
        registered = state;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public void setAbsorbedHpPercent(int percent) {
        absorbedHpPercent = percent;
    }

    public boolean isValid(int itemId) {
        return this.itemId == itemId && absorbedHpPercent < 50;
    }

    @Override
    public String toString() {
        return "SoulCrystalAbsorbInfo{" +
                "registered=" + registered +
                ", itemId=" + itemId +
                ", absorbedHpPercent=" + absorbedHpPercent +
                '}';
    }
}