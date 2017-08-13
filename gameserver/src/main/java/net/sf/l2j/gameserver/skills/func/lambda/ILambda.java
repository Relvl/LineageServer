package net.sf.l2j.gameserver.skills.func.lambda;

import net.sf.l2j.gameserver.skills.func.Env;

/**  */
@FunctionalInterface
public interface ILambda {
    double calc(Env env);
}