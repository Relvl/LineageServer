package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerLevel extends ACondition {
    private final int level;

    public ConditionPlayerLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getCharacter().getLevel() >= level;
    }
}