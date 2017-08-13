package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerWeight extends ACondition {
    private final int weight;

    public ConditionPlayerWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public boolean testImpl(Env env) {
        L2PcInstance player = env.getPlayer();
        if (player != null && player.getMaxLoad() > 0) {
            int weightproc = player.getCurrentLoad() * 100 / player.getMaxLoad();
            return weightproc < weight;
        }
        return true;
    }
}