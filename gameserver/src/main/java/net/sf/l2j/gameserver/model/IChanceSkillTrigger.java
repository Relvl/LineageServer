package net.sf.l2j.gameserver.model;

public interface IChanceSkillTrigger {
    boolean triggersChanceSkill();

    int getTriggeredChanceId();

    int getTriggeredChanceLevel();

    ChanceCondition getTriggeredChanceCondition();

    default boolean isSkill() { return false; }

    default boolean isEffect() { return false; }
}