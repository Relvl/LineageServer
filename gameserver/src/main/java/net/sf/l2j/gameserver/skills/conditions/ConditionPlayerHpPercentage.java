package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerHpPercentage extends ACondition {
    private final double hpPercent;

    public ConditionPlayerHpPercentage(double hpPercent) {
        this.hpPercent = hpPercent;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getCharacter().getCurrentHp() <= env.getCharacter().getMaxHp() * hpPercent;
    }
}
