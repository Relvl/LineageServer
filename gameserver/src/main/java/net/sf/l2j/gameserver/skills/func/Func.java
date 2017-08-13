package net.sf.l2j.gameserver.skills.func;

import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.conditions.ACondition;
import net.sf.l2j.gameserver.skills.func.lambda.ILambda;

/**
 * Функция калькулятора. Несколько функций последовательно вычисляют конечное значение стата.
 * <p>
 * Функции с меньшим порядком вычисляются первыми.
 * Функции с одинаковым порядком вычисляются неупорядоченно.
 * <p>
 * <b>Обычно</b>, функции добавления и вычитания имеют самый низкий порядок.
 * За ними следуют функции умножения и деления.
 * За ними следуют все остальные (обычно нелинейные) функции.
 */
public abstract class Func {
    protected final Stats stat;
    protected final int order;
    protected final Object owner;

    protected ACondition condition;
    protected ILambda lambda;

    protected Func(Stats pStat, int pOrder, Object owner, ILambda lambda) {
        stat = pStat;
        order = pOrder;
        this.owner = owner;
        this.lambda = lambda;
    }

    public abstract void calc(Env env);

    public final Stats getStat() { return stat; }

    public final int getOrder() { return order; }

    public final Object getOwner() { return owner; }

    public final ACondition getCondition() { return condition; }

    public final void setCondition(ACondition condition) { this.condition = condition; }

    public final ILambda getLambda() { return lambda; }
}
