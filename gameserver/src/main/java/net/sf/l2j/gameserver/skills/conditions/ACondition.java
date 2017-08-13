package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public abstract class ACondition implements IConditionListener {
    protected static final ACondition[] EMPTY_CONDITIONS = new ACondition[0];

    private IConditionListener conditionListener;
    private String message;
    private int messageId;
    private boolean additionalName;
    private boolean result;

    public final void setMessage(String msg) { message = msg; }

    public final String getMessage() { return message; }

    public final void setMessageId(int msgId) { messageId = msgId; }

    public final int getMessageId() { return messageId; }

    public final void addName() { additionalName = true; }

    public final boolean isAddName() { return additionalName; }

    void setListener(IConditionListener listener) {
        conditionListener = listener;
        notifyChanged();
    }

    final IConditionListener getListener() { return conditionListener; }

    public final boolean test(Env env) {
        boolean res = testImpl(env);
        if (conditionListener != null && res != result) {
            result = res;
            notifyChanged();
        }
        return res;
    }

    abstract boolean testImpl(Env env);

    @Override
    public void notifyChanged() {
        if (conditionListener != null) {
            conditionListener.notifyChanged();
        }
    }
}