package net.sf.l2j.gameserver.skills.func.lambda;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.func.Func;

import java.util.ArrayList;
import java.util.List;

/** Вычисляет все заявленные функции, не меняя окружение. */
public final class LambdaCalc implements ILambda {
    private final List<Func> functions = new ArrayList<>();

    @Override
    public double calc(Env env) {
        double saveValue = env.getValue();
        try {
            env.setValue(0);
            for (Func func : functions) {
                func.calc(env);
            }
            return env.getValue();
        }
        finally {
            env.setValue(saveValue);
        }
    }

    public void addFunc(Func func) { functions.add(func); }

    public List<Func> getFuncs() { return functions; }
}