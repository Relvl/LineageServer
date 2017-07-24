package net.sf.l2j.gameserver.model.soulcrystal;

public final class SoulCrystalData {
    private final int level;
    private final int crystalItemId;
    private final int stagedItemId;
    private final int brokenItemId;

    public SoulCrystalData(int level, int crystalItemId, int stagedItemId, int brokenItemId) {
        this.level = level;
        this.crystalItemId = crystalItemId;
        this.stagedItemId = stagedItemId;
        this.brokenItemId = brokenItemId;
    }

    public int getLevel() { return level; }

    public int getCrystalItemId() { return crystalItemId; }

    public int getStagedItemId() { return stagedItemId; }

    public int getBrokenItemId() { return brokenItemId; }
}