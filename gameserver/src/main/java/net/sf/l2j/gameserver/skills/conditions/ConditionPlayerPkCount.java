package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerPkCount extends ACondition {
    public final int pk;

    public ConditionPlayerPkCount(int pk) {
        this.pk = pk;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getPlayer() != null && env.getPlayer().getPkKills() <= pk;
    }
}