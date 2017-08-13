package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerPledgeClass extends ACondition {
    private final int pledgeClass;

    public ConditionPlayerPledgeClass(int pledgeClass) {
        this.pledgeClass = pledgeClass;
    }

    @Override
    public boolean testImpl(Env env) {
        if (env.getPlayer() == null) { return false; }
        if (env.getPlayer().getClan() == null) { return false; }
        if (pledgeClass == -1) { return env.getPlayer().isClanLeader(); }
        return env.getPlayer().getPledgeClass() >= pledgeClass;
    }
}