package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerHp extends ACondition {
    private final int hp;

    public ConditionPlayerHp(int hp) {
        this.hp = hp;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getCharacter().getCurrentHp() * 100 / env.getCharacter().getMaxHp() <= hp;
    }
}