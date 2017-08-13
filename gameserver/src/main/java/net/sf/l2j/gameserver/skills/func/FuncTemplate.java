package net.sf.l2j.gameserver.skills.func;

import net.sf.l2j.gameserver.skills.conditions.ACondition;
import net.sf.l2j.gameserver.skills.func.lambda.ILambda;
import net.sf.l2j.gameserver.skills.Stats;

public final class FuncTemplate {
    private final EFunction function;
    private final Stats stat;
    private final int order;
    private final ILambda lambda;
    private final ACondition attachCond;
    private final ACondition applyCond;

    public FuncTemplate(ACondition attachCond, ACondition applyCond, EFunction function, Stats stat, int order, ILambda lambda) {
        this.attachCond = attachCond;
        this.applyCond = applyCond;
        this.stat = stat;
        this.order = order;
        this.lambda = lambda;
        this.function = function;
    }

    public Func getFunc(Env env, Object owner) {
        if (attachCond != null && !attachCond.test(env)) { return null; }
        Func funcInstance = function.getFuncFactory().make(stat, order, owner, lambda);
        if (applyCond != null) {
            funcInstance.setCondition(applyCond);
        }
        return funcInstance;
    }
}