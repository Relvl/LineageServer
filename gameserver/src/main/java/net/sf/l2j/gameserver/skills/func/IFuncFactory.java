package net.sf.l2j.gameserver.skills.func;

import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.lambda.ILambda;

/**
 * @author Johnson / 13.08.2017
 */
@FunctionalInterface
public interface IFuncFactory<F extends Func> {
    /**  */
    F make(Stats pStat, int pOrder, Object owner, ILambda lambda);
}
