package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.skills.Stats;

public class NpcStat extends CharStat {
    public NpcStat(L2Npc activeChar) {
        super(activeChar);
    }

    @Override
    public byte getLevel() {
        return getActiveChar().getTemplate().getLevel();
    }

    @Override
    public L2Npc getActiveChar() {
        return (L2Npc) super.getActiveChar();
    }

    @Override
    public int getWalkSpeed() {
        return (int) calcStat(Stats.WALK_SPEED, getActiveChar().getTemplate().getBaseWalkSpd(), null, null);
    }

    @Override
    public float getMovementSpeedMultiplier() {
        if (getActiveChar().isRunning()) { return getRunSpeed() * 1f / getActiveChar().getTemplate().getBaseRunSpd(); }

        return getWalkSpeed() * 1f / getActiveChar().getTemplate().getBaseWalkSpd();
    }
}