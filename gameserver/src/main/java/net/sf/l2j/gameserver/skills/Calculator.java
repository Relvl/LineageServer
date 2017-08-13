package net.sf.l2j.gameserver.skills;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.func.Func;

import java.util.ArrayList;
import java.util.List;

public final class Calculator {
    private static final Func[] EMPTY_FUNCS = new Func[0];
    private Func[] functions = EMPTY_FUNCS;

    public Calculator() {}

    public Calculator(Calculator calculator) {
        functions = calculator.functions;
    }

    public int size() { return functions.length; }

    public synchronized void addFunc(Func func) {
        Func[] funcs = functions;
        Func[] tmp = new Func[funcs.length + 1];

        int i;
        for (i = 0; i < funcs.length && func.getOrder() >= funcs[i].getOrder(); i++) {
            tmp[i] = funcs[i];
        }
        tmp[i] = func;

        for (; i < funcs.length; i++) { tmp[i + 1] = funcs[i]; }

        functions = tmp;
    }

    public synchronized void removeFunc(Func func) {
        Func[] funcs = functions;
        Func[] tmp = new Func[funcs.length - 1];

        int i;
        for (i = 0; i < funcs.length && func != funcs[i]; i++) { tmp[i] = funcs[i]; }
        if (i == funcs.length) { return; }
        for (i++; i < funcs.length; i++) {
            tmp[i - 1] = funcs[i];
        }
        functions = tmp.length == 0 ? EMPTY_FUNCS : tmp;
    }

    public synchronized List<Stats> removeOwner(Object owner) {
        List<Stats> modifiedStats = new ArrayList<>();
        for (Func func : functions) {
            if (func.getOwner() == owner) {
                modifiedStats.add(func.getStat());
                removeFunc(func);
            }
        }
        return modifiedStats;
    }

    public void calc(Env env) {
        for (Func func : functions) {
            func.calc(env);
        }
    }
}