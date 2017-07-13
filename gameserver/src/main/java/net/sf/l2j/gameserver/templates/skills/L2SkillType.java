package net.sf.l2j.gameserver.templates.skills;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.l2skills.*;
import net.sf.l2j.gameserver.templates.StatsSet;

public enum L2SkillType {
    // Damage
    PDAM,
    FATAL,
    MDAM,
    CPDAMPERCENT,
    MANADAM,
    DOT,
    MDOT,
    DRAIN_SOUL,
    DRAIN(L2SkillDrain.class),
    DEATHLINK,
    BLOW,
    SIGNET(L2SkillSignet.class),
    SIGNET_CASTTIME(L2SkillSignetCasttime.class),
    SEED(L2SkillSeed.class),

    // Disablers
    BLEED,
    POISON,
    STUN,
    ROOT,
    CONFUSION,
    FEAR,
    SLEEP,
    MUTE,
    PARALYZE,
    WEAKNESS,

    // hp, mp, cp
    HEAL,
    MANAHEAL,
    COMBATPOINTHEAL,
    HOT,
    MPHOT,
    CPHOT,
    BALANCE_LIFE,
    HEAL_STATIC,
    MANARECHARGE,
    HEAL_PERCENT,
    CPHEAL_PERCENT,
    MANAHEAL_PERCENT,

    GIVE_SP,

    // Aggro
    AGGDAMAGE,
    AGGREDUCE,
    AGGREMOVE,
    AGGREDUCE_CHAR,
    AGGDEBUFF,

    // Fishing
    FISHING,
    PUMPING,
    REELING,

    // MISC
    UNLOCK,
    UNLOCK_SPECIAL,
    DELUXE_KEY_UNLOCK,
    ENCHANT_ARMOR,
    ENCHANT_WEAPON,
    SOULSHOT,
    SPIRITSHOT,
    SIEGEFLAG(L2SkillSiegeFlag.class),
    TAKECASTLE,
    WEAPON_SA,
    SOW,
    HARVEST,
    GET_PLAYER,
    DUMMY,
    INSTANT_JUMP,

    // Creation
    COMMON_CRAFT,
    DWARVEN_CRAFT,
    CREATE_ITEM(L2SkillCreateItem.class),
    EXTRACTABLE,
    EXTRACTABLE_FISH,

    // Summons
    SUMMON(L2SkillSummon.class),
    FEED_PET,
    DEATHLINK_PET,
    STRSIEGEASSAULT,
    ERASE,
    BETRAY,
    SPAWN(L2SkillSpawn.class),

    // Cancel
    CANCEL,
    MAGE_BANE,
    WARRIOR_BANE,

    NEGATE,
    CANCEL_DEBUFF,

    BUFF,
    DEBUFF,
    PASSIVE,
    CONT,

    RESURRECT,
    CHARGEDAM(L2SkillChargeDmg.class),
    MHOT,
    DETECT_WEAKNESS,
    LUCK,
    RECALL(L2SkillTeleport.class),
    TELEPORT(L2SkillTeleport.class),
    SUMMON_FRIEND,
    REFLECT,
    SPOIL,
    SWEEP,
    FAKE_DEATH,
    UNBLEED,
    UNPOISON,
    UNDEAD_DEFENSE,
    BEAST_FEED,
    FUSION,

    CHANGE_APPEARANCE(L2SkillAppearance.class),

    // Skill is done within the core.
    COREDONE,

    // unimplemented
    NOTDONE;

    private final Class<? extends L2Skill> skillClass;

    L2SkillType() {
        skillClass = L2SkillDefault.class;
    }

    L2SkillType(Class<? extends L2Skill> classType) {
        skillClass = classType;
    }

    public L2Skill makeSkill(StatsSet set) {
        try {
            return skillClass.getConstructor(StatsSet.class).newInstance(set);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}