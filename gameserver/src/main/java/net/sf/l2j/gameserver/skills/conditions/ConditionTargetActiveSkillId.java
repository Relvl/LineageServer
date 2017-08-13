package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionTargetActiveSkillId extends ACondition {
    private final int skillId;
    private final int skillLevel;

    public ConditionTargetActiveSkillId(int skillId) {
        this.skillId = skillId;
        skillLevel = -1;
    }

    public ConditionTargetActiveSkillId(int skillId, int skillLevel) {
        this.skillId = skillId;
        this.skillLevel = skillLevel;
    }

    @Override
    public boolean testImpl(Env env) {
        for (L2Skill sk : env.getTarget().getAllSkills()) {
            if (sk != null) {
                if (sk.getId() == skillId) {
                    if (skillLevel == -1 || skillLevel <= sk.getLevel()) { return true; }
                }
            }
        }
        return false;
    }
}