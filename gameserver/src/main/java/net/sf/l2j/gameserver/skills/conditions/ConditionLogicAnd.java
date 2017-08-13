package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionLogicAnd extends ACondition {
    public ACondition[] conditions = EMPTY_CONDITIONS;

    public void add(ACondition condition) {
        if (condition == null) { return; }
        if (getListener() != null) { condition.setListener(this); }
        int len = conditions.length;
        ACondition[] tmp = new ACondition[len + 1];
        System.arraycopy(conditions, 0, tmp, 0, len);
        tmp[len] = condition;
        conditions = tmp;
    }

    @Override
    void setListener(IConditionListener listener) {
        if (listener != null) {
            for (ACondition condition : conditions) {
                condition.setListener(this);
            }
        }
        else {
            for (ACondition condition : conditions) {
                condition.setListener(null);
            }
        }
        super.setListener(listener);
    }

    @Override
    public boolean testImpl(Env env) {
        for (ACondition condition : conditions) {
            if (!condition.test(env)) { return false; }
        }
        return true;
    }
}