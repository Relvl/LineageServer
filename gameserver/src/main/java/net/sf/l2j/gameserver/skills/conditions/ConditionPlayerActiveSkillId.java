package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerActiveSkillId extends ACondition {
    private final int skillId;
    private final int skillLevel;

    public ConditionPlayerActiveSkillId(int skillId) {
        this.skillId = skillId;
        this.skillLevel = -1;
    }

    public ConditionPlayerActiveSkillId(int skillId, int skillLevel) {
        this.skillId = skillId;
        this.skillLevel = skillLevel;
    }

    @Override
    public boolean testImpl(Env env) {
        for (L2Skill sk : env.getCharacter().getAllSkills()) {
            if (sk != null && sk.getId() == skillId) {
                if (skillLevel == -1 || skillLevel <= sk.getLevel()) { return true; }
            }
        }
        return false;
    }
}