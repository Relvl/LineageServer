package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerIsHero extends ACondition {
    private final boolean val;

    public ConditionPlayerIsHero(boolean val) {
        this.val = val;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getPlayer() != null && env.getPlayer().isHero() == val;
    }
}