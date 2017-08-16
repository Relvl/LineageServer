package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncMaxCpMul extends Func {
    public FuncMaxCpMul() { super(Stats.MAX_CP, 0x20, null, null); }

    @Override
    public void calc(Env env) {
        env.mulValue(Formulas.CONbonus[env.getCharacter().getCON()]);
    }
}