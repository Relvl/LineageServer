package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.skills.effects.EffectSeed;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class L2SkillSeed extends L2Skill {
    public L2SkillSeed(StatsSet set) {
        super(set);
    }

    @Override
    public void useSkill(L2Character caster, L2Object[] targets) {
        if (caster.isAlikeDead()) { return; }

        // Update Seeds Effects
        for (L2Object obj : targets) {
            if (!(obj instanceof L2Character)) { continue; }

            L2Character target = (L2Character) obj;
            if (target.isAlikeDead() && getTargetType() != ESkillTargetType.TARGET_CORPSE_MOB) { continue; }

            EffectSeed oldEffect = (EffectSeed) target.getFirstEffect(getId());
            if (oldEffect == null) { getEffects(caster, target); }
            else { oldEffect.increasePower(); }

            L2Effect[] effects = target.getAllEffects();
            for (L2Effect effect : effects) { if (effect.getEffectType() == L2EffectType.SEED) { effect.rescheduleEffect(); } }
        }
    }
}