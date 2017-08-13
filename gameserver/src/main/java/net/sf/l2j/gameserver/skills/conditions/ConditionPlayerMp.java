package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerMp extends ACondition {
    private final int mp;

    public ConditionPlayerMp(int mp) {
        this.mp = mp;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getCharacter().getCurrentMp() * 100 / env.getCharacter().getMaxMp() <= mp;
    }
}