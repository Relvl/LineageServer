package net.sf.l2j.gameserver.skills.func.lambda;

import net.sf.l2j.gameserver.skills.func.Env;

/** ¬сегда возвращает указанное заранее значение. */
public final class LambdaConst implements ILambda {
    private final double value;

    public LambdaConst(double value) {
        this.value = value;
    }

    @Override
    public double calc(Env env) { return value; }
}