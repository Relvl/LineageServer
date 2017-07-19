package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.model.skill.L2Skill;

/**
 * Simple class containing all neccessary information to maintain valid timestamps and reuse for skills upon relog. Filter this carefully as it becomes redundant to store reuse for small delays.
 *
 * @author Yesod
 */
public class TimeStamp {
    private final int skillId;
    private final int skillLvl;
    private final long reuse;
    private final long stamp;

    public TimeStamp(L2Skill skill, long reuse) {
        skillId = skill.getId();
        skillLvl = skill.getLevel();
        this.reuse = reuse;
        stamp = System.currentTimeMillis() + reuse;
    }

    public TimeStamp(L2Skill skill, long reuse, long systime) {
        skillId = skill.getId();
        skillLvl = skill.getLevel();
        this.reuse = reuse;
        stamp = systime;
    }

    public long getStamp() {
        return stamp;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getSkillLvl() {
        return skillLvl;
    }

    public long getReuse() {
        return reuse;
    }

    public long getRemaining() {
        return Math.max(stamp - System.currentTimeMillis(), 0);
    }

    public boolean hasNotPassed() {
        return System.currentTimeMillis() < stamp;
    }
}
