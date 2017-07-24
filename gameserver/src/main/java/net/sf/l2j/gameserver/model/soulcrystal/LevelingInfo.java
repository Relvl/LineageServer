package net.sf.l2j.gameserver.model.soulcrystal;

public final class LevelingInfo {
    public enum AbsorbCrystalType {
        LAST_HIT,
        FULL_PARTY,
        PARTY_ONE_RANDOM
    }

    private final AbsorbCrystalType absorbCrystalType;
    private final boolean skillRequired;
    private final int chanceStage;
    private final int chanceBreak;
    private final int[] levelList;

    public LevelingInfo(AbsorbCrystalType absorbCrystalType, boolean skillRequired, int chanceStage, int chanceBreak, int[] levelList) {
        this.absorbCrystalType = absorbCrystalType;
        this.skillRequired = skillRequired;
        this.chanceStage = chanceStage;
        this.chanceBreak = chanceBreak;
        this.levelList = levelList;
    }

    public AbsorbCrystalType getAbsorbCrystalType() { return absorbCrystalType; }

    public boolean isSkillRequired() { return skillRequired; }

    public int getChanceStage() { return chanceStage; }

    public int getChanceBreak() { return chanceBreak; }

    public boolean isInLevelList(int level) {
        for (int lvl : levelList) {
            if (lvl == level) { return true; }
        }
        return false;
    }
}