package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerSex extends ACondition {
    // male 0 female 1
    private final int sex;

    public ConditionPlayerSex(int sex) {
        this.sex = sex;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getPlayer() != null && (env.getPlayer().getAppearance().isFemale() ? 1 : 0) == sex;
    }
}