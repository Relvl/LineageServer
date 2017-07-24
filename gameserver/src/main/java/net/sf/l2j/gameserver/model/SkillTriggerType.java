package net.sf.l2j.gameserver.model;

/**
 * @author Johnson / 24.07.2017
 */
public enum SkillTriggerType {
    // You hit an enemy
    ON_HIT(0x00001),
    // You hit an enemy - was crit
    ON_CRIT(0x00002),
    // You cast a skill
    ON_CAST(0x00004),
    // You cast a skill - it was a physical one
    ON_PHYSICAL(0x00008),
    // You cast a skill - it was a magic one
    ON_MAGIC(0x00010),
    // You cast a skill - it was a magic one - good magic
    ON_MAGIC_GOOD(0x00020),
    // You cast a skill - it was a magic one - offensive magic
    ON_MAGIC_OFFENSIVE(0x00040),
    // You are attacked by enemy
    ON_ATTACKED(0x00080),
    // You are attacked by enemy - by hit
    ON_ATTACKED_HIT(0x00100),
    // You are attacked by enemy - by hit - was crit
    ON_ATTACKED_CRIT(0x00200),
    // A skill was casted on you
    ON_HIT_BY_SKILL(0x00400),
    // An evil skill was casted on you
    ON_HIT_BY_OFFENSIVE_SKILL(0x00800),
    // A good skill was casted on you
    ON_HIT_BY_GOOD_MAGIC(0x01000),
    // Evading melee attack
    ON_EVADED_HIT(0x02000),
    // Effect only - on start
    ON_START(0x04000),
    // Effect only - each second
    ON_ACTION_TIME(0x08000),
    // Effect only - on exit
    ON_EXIT(0x10000);

    private final int mask;

    SkillTriggerType(int mask) {
        this.mask = mask;
    }

    public final boolean check(int event) { return (mask & event) != 0; }

    public int getMask() { return mask; }
}
