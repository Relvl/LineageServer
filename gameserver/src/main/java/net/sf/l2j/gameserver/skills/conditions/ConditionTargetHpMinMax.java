package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

/**
 * Used for Trap skills.
 *
 * @author Tryskell
 */
public final class ConditionTargetHpMinMax extends ACondition {
    private final int minHp;
    private final int maxHp;

    public ConditionTargetHpMinMax(int minHp, int maxHp) {
        this.minHp = minHp;
        this.maxHp = maxHp;
    }

    @Override
    public boolean testImpl(Env env) {
        if (env.getTarget() == null) { return false; }
        int currentHp = (int) env.getTarget().getCurrentHp() * 100 / env.getTarget().getMaxHp();
        return currentHp >= minHp && currentHp <= maxHp;
    }
}