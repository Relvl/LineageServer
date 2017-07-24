package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.model.skill.chance.ChanceCondition;

public interface IChanceSkillTrigger {
    boolean triggersChanceSkill();

    int getTriggeredChanceId();

    int getTriggeredChanceLevel();

    ChanceCondition getTriggeredChanceCondition();

    default boolean isSkill() { return false; }

    default boolean isEffect() { return false; }
}