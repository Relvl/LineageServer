package net.sf.l2j.gameserver.model;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.templates.StatsSet;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author kombat
 */
public final class ChanceCondition {
    private static final Logger _log = Logger.getLogger(ChanceCondition.class.getName());

    private final SkillTriggerType skillTriggerType;
    private final int chance;

    private ChanceCondition(SkillTriggerType skillTriggerType, int chance) {
        this.skillTriggerType = skillTriggerType;
        this.chance = chance;
    }

    public static ChanceCondition parse(StatsSet set) {
        try {
            SkillTriggerType trigger = set.getEnum("chanceType", SkillTriggerType.class, null);
            int chance = set.getInteger("activationChance", -1);

            if (trigger != null) { return new ChanceCondition(trigger, chance); }
        }
        catch (RuntimeException e) {
            _log.log(Level.WARNING, "", e);
        }
        return null;
    }

    public static ChanceCondition parse(String chanceType, int chance) {
        try {
            if (chanceType == null) { return null; }
            SkillTriggerType trigger = Enum.valueOf(SkillTriggerType.class, chanceType);
            return new ChanceCondition(trigger, chance);
        }
        catch (RuntimeException e) {
            _log.log(Level.WARNING, "", e);
        }
        return null;
    }

    public boolean trigger(int event) { return skillTriggerType.check(event) && (chance < 0 || Rnd.get(100) < chance); }

    @Override
    public String toString() {
        return "Trigger[" + chance + ";" + skillTriggerType + "]";
    }
}