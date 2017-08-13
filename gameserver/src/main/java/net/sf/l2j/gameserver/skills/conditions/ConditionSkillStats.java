package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Stats;

public final class ConditionSkillStats extends ACondition {
    private final Stats stat;

    public ConditionSkillStats(Stats stat) {
        this.stat = stat;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getSkill() != null && env.getSkill().getStat() == stat;
    }
}