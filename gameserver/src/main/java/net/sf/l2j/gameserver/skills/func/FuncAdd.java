package net.sf.l2j.gameserver.skills.func;

import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.lambda.ILambda;

public class FuncAdd extends Func {
    public FuncAdd(Stats pStat, int pOrder, Object owner, ILambda lambda) {
        super(pStat, pOrder, owner, lambda);
    }

    @Override
    public void calc(Env env) {
        if (condition == null || condition.test(env)) {
            env.addValue(lambda.calc(env));
        }
    }
}