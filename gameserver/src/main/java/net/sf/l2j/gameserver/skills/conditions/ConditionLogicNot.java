package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionLogicNot extends ACondition {
    private final ACondition condition;

    public ConditionLogicNot(ACondition condition) {
        this.condition = condition;
        if (getListener() != null) { this.condition.setListener(this); }
    }

    @Override
    void setListener(IConditionListener listener) {
        if (listener != null) { condition.setListener(this); }
        else { condition.setListener(null); }
        super.setListener(listener);
    }

    @Override
    public boolean testImpl(Env env) {
        return !condition.test(env);
    }
}
