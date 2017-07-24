package net.sf.l2j.gameserver.model.skill.chance;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.templates.StatsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChanceCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChanceCondition.class);

    private final ESkillTriggerType skillTriggerType;
    private final int chance;

    private ChanceCondition(ESkillTriggerType skillTriggerType, int chance) {
        this.skillTriggerType = skillTriggerType;
        this.chance = chance;
    }

    public static ChanceCondition parse(StatsSet set) {
        try {
            ESkillTriggerType trigger = set.getEnum("chanceType", ESkillTriggerType.class, null);
            int chance = set.getInteger("activationChance", -1);

            if (trigger != null) { return new ChanceCondition(trigger, chance); }
        }
        catch (RuntimeException e) {
            LOGGER.error("", e);
        }
        return null;
    }

    public static ChanceCondition parse(String chanceType, int chance) {
        try {
            if (chanceType == null) { return null; }
            ESkillTriggerType trigger = Enum.valueOf(ESkillTriggerType.class, chanceType);
            return new ChanceCondition(trigger, chance);
        }
        catch (RuntimeException e) {
            LOGGER.error("", e);
        }
        return null;
    }

    public boolean trigger(int event) { return skillTriggerType.check(event) && (chance < 0 || Rnd.get(100) < chance); }

    @Override
    public String toString() { return "Trigger[" + chance + ";" + skillTriggerType + "]"; }
}