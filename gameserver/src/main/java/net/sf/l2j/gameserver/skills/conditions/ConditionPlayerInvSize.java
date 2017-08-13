package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerInvSize extends ACondition {
    private final int size;

    public ConditionPlayerInvSize(int size) {
        this.size = size;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getPlayer() == null || env.getPlayer().getInventory().getSize() <= (env.getPlayer().getInventoryLimit() - size);
    }
}