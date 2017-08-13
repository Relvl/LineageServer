package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionGameChance extends ACondition {
    private final int chance;

    public ConditionGameChance(int chance) { this.chance = chance; }

    @Override
    public boolean testImpl(Env env) { return Rnd.get(100) < chance; }
}
