package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.Env;

public abstract class Condition implements ConditionListener {
    private ConditionListener _listener;
    private String _msg;
    private int _msgId;
    private boolean _addName;
    private boolean _result;

    public final void setMessage(String msg) {
        _msg = msg;
    }

    public final String getMessage() {
        return _msg;
    }

    public final void setMessageId(int msgId) {
        _msgId = msgId;
    }

    public final int getMessageId() {
        return _msgId;
    }

    public final void addName() {
        _addName = true;
    }

    public final boolean isAddName() {
        return _addName;
    }

    void setListener(ConditionListener listener) {
        _listener = listener;
        notifyChanged();
    }

    final ConditionListener getListener() {
        return _listener;
    }

    public final boolean test(Env env) {
        boolean res = testImpl(env);
        if (_listener != null && res != _result) {
            _result = res;
            notifyChanged();
        }
        return res;
    }

    abstract boolean testImpl(Env env);

    @Override
    public void notifyChanged() {
        if (_listener != null) { _listener.notifyChanged(); }
    }
}