package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.IChanceSkillTrigger;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.skill.chance.ChanceCondition;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectChanceSkillTrigger extends L2Effect implements IChanceSkillTrigger {
    private final int triggeredId;
    private final int triggeredLevel;
    private final ChanceCondition chanceCondition;

    public EffectChanceSkillTrigger(Env env, EffectTemplate template) {
        super(env, template);

        triggeredId = template.triggeredId;
        triggeredLevel = template.triggeredLevel;
        chanceCondition = template.chanceCondition;
    }

    @Override
    public L2EffectType getEffectType() { return L2EffectType.CHANCE_SKILL_TRIGGER; }

    @Override
    public boolean onStart() {
        getEffected().addChanceTrigger(this);
        getEffected().onStartChanceEffect();
        return super.onStart();
    }

    @Override
    public boolean onActionTime() {
        getEffected().onActionTimeChanceEffect();
        return false;
    }

    @Override
    public void onExit() {
        // trigger only if effect in use and successfully ticked to the end
        if (getInUse() && getCount() == 0) { getEffected().onExitChanceEffect(); }
        getEffected().removeChanceEffect(this);
        super.onExit();
    }

    @Override
    public int getTriggeredChanceId() { return triggeredId; }

    @Override
    public int getTriggeredChanceLevel() { return triggeredLevel; }

    @Override
    public boolean triggersChanceSkill() { return triggeredId > 1; }

    @Override
    public ChanceCondition getTriggeredChanceCondition() { return chanceCondition; }

    @Override
    public boolean isEffect() { return true; }
}