package net.sf.l2j.gameserver.skills.func.lambda;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.skills.func.Env;

/** Вычисляет случайное значение в пределах значения переданного вычислятеля. */
public final class LambdaRnd implements ILambda {
    private final ILambda max;
    private final boolean linear;

    public LambdaRnd(ILambda max, boolean linear) {
        this.max = max;
        this.linear = linear;
    }

    @Override
    public double calc(Env env) {
        return max.calc(env) * ((linear) ? Rnd.nextDouble() : Rnd.nextGaussian());
    }
}