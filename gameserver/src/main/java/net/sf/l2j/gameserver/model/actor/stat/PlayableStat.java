package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.model.zone.type.L2SwampZone;

public class PlayableStat extends CharStat {
    public PlayableStat(L2Playable activeChar) {
        super(activeChar);
    }

    public boolean addExp(long value) {
        if ((getExp() + value) < 0) { return true; }
        if (getExp() + value >= getExpForLevel(Experience.MAX_LEVEL)) {
            value = getExpForLevel(Experience.MAX_LEVEL) - 1 - getExp();
        }
        setExp(getExp() + value);
        byte level;
        for (level = 1; level <= Experience.MAX_LEVEL; level++) {
            if (getExp() >= getExpForLevel(level)) { continue; }
            level--;
            break;
        }
        if (level != getLevel()) {
            addLevel((byte) (level - getLevel()));
        }
        return true;
    }

    public boolean removeExp(long value) {
        if ((getExp() - value) < 0) { value = getExp() - 1; }
        setExp(getExp() - value);
        byte level;
        for (level = 1; level <= Experience.MAX_LEVEL; level++) {
            if (getExp() >= getExpForLevel(level)) { continue; }
            level--;
            break;
        }
        if (level != getLevel()) {
            addLevel((byte) (level - getLevel()));
        }
        return true;
    }

    public boolean addExpAndSp(long addToExp, int addToSp) {
        boolean expAdded = false;
        if (addToExp >= 0) {
            expAdded = addExp(addToExp);
        }
        boolean spAdded = false;
        if (addToSp >= 0) {
            spAdded = addSp(addToSp);
        }
        return expAdded || spAdded;
    }

    public boolean removeExpAndSp(long removeExp, int removeSp) {
        boolean expRemoved = false;
        if (removeExp > 0) { expRemoved = removeExp(removeExp); }
        boolean spRemoved = false;
        if (removeSp > 0) { spRemoved = removeSp(removeSp); }
        return expRemoved || spRemoved;
    }

    public boolean addLevel(byte value) {
        if (getLevel() + value > Experience.MAX_LEVEL - 1) {
            if (getLevel() < Experience.MAX_LEVEL - 1) {
                value = (byte) (Experience.MAX_LEVEL - 1 - getLevel());
            }
            else { return false; }
        }
        boolean levelIncreased = getLevel() + value > getLevel();
        value += getLevel();
        setLevel(value);
        // Sync up exp with current level
        if (getExp() >= getExpForLevel(getLevel() + 1) || getExpForLevel(getLevel()) > getExp()) { setExp(getExpForLevel(getLevel())); }
        if (!levelIncreased) { return false; }
        getActiveChar().getStatus().setCurrentHp(getActiveChar().getStat().getMaxHp());
        getActiveChar().getStatus().setCurrentMp(getActiveChar().getStat().getMaxMp());
        return true;
    }

    public boolean addSp(int value) {
        if (value < 0) { return false; }
        int currentSp = getSp();
        if (currentSp == Integer.MAX_VALUE) { return false; }
        if (currentSp > Integer.MAX_VALUE - value) { value = Integer.MAX_VALUE - currentSp; }
        setSp(currentSp + value);
        return true;
    }

    public boolean removeSp(int value) {
        int currentSp = getSp();
        if (currentSp < value) { value = currentSp; }
        setSp(getSp() - value);
        return true;
    }

    public long getExpForLevel(int level) {
        return level;
    }

    @Override
    public int getRunSpeed() {
        int val = super.getRunSpeed();
        if (getActiveChar().isInsideZone(ZoneId.WATER)) { val *= 0.41; }
        if (getActiveChar().isInsideZone(ZoneId.SWAMP)) {
            L2SwampZone zone = ZoneManager.getZone(getActiveChar(), L2SwampZone.class);
            int bonus = zone == null ? 0 : zone.getMoveBonus();
            double dbonus = bonus / 100.0; // %
            val += val * dbonus;
        }

        return val;
    }

    @Override
    public L2Playable getActiveChar() {
        return (L2Playable) super.getActiveChar();
    }
}