package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerActiveEffectId extends ACondition {
    private final int effectId;
    private final int effectLevel;

    public ConditionPlayerActiveEffectId(int effectId) {
        this.effectId = effectId;
        this.effectLevel = -1;
    }

    public ConditionPlayerActiveEffectId(int effectId, int effectLevel) {
        this.effectId = effectId;
        this.effectLevel = effectLevel;
    }

    @Override
    public boolean testImpl(Env env) {
        L2Effect e = env.getCharacter().getFirstEffect(effectId);
        return e != null && (effectLevel == -1 || effectLevel <= e.getSkill().getLevel());
    }
}