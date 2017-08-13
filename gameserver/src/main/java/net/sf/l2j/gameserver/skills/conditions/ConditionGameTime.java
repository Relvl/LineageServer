package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public final class ConditionGameTime extends ACondition {
    private final boolean night;

    public ConditionGameTime(boolean night) { this.night = night; }

    @Override
    public boolean testImpl(Env env) { return GameTimeTaskManager.getInstance().isNight() == night; }
}