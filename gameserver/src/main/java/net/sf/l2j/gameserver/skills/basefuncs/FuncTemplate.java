package net.sf.l2j.gameserver.skills.basefuncs;

import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class FuncTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuncTemplate.class);

    public Condition attachCond;
    public Condition applayCond;
    public final Class<?> func;
    public final Constructor<?> constructor;
    public final Stats stat;
    public final int order;
    public final Lambda lambda;

    public FuncTemplate(Condition pAttachCond, Condition pApplayCond, String pFunc, Stats pStat, int pOrder, Lambda pLambda) {
        attachCond = pAttachCond;
        applayCond = pApplayCond;
        stat = pStat;
        order = pOrder;
        lambda = pLambda;

        try {
            func = Class.forName("net.sf.l2j.gameserver.skills.basefuncs.Func" + pFunc);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            constructor = func.getConstructor(
                    Stats.class, // stats to update
                    Integer.TYPE, // order of execution
                    Object.class, // owner
                    Lambda.class
            );
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Func getFunc(Env env, Object owner) {
        if (attachCond != null && !attachCond.test(env)) { return null; }

        try {
            Func funcInstance = (Func) constructor.newInstance(stat, order, owner, lambda);
            if (applayCond != null) { funcInstance.setCondition(applayCond); }
            return funcInstance;
        }
        catch (IllegalAccessException e) {
            LOGGER.error("", e);
            return null;
        }
        catch (InstantiationException e) {
            LOGGER.error("", e);
            return null;
        }
        catch (InvocationTargetException e) {
            LOGGER.error("", e);
            return null;
        }
    }
}