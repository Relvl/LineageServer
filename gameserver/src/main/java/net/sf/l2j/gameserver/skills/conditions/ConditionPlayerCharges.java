package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerCharges extends ACondition {
    private final int charges;

    public ConditionPlayerCharges(int charges) {
        this.charges = charges;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getPlayer() != null && env.getPlayer().getCharges() >= charges;
    }
}