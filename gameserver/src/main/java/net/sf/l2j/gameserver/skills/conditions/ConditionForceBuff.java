package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.skill.SkillConst;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.effects.EffectFusion;

public final class ConditionForceBuff extends ACondition {

    private final byte[] forces;

    public ConditionForceBuff(byte[] forces) {
        this.forces = forces;
    }

    @Override
    public boolean testImpl(Env env) {
        if (forces[0] > 0) {
            L2Effect force = env.getCharacter().getFirstEffect(SkillConst.BATTLE_FORCE);
            if (force == null || ((EffectFusion) force)._effect < forces[0]) { return false; }
        }

        if (forces[1] > 0) {
            L2Effect force = env.getCharacter().getFirstEffect(SkillConst.SPELL_FORCE);
            if (force == null || ((EffectFusion) force)._effect < forces[1]) { return false; }
        }
        return true;
    }
}