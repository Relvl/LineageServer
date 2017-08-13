package net.sf.l2j.gameserver.skills.conditions;

@FunctionalInterface
public interface IConditionListener {
    void notifyChanged();
}