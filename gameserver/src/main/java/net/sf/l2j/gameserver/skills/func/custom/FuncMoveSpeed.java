package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncMoveSpeed extends Func {
    public FuncMoveSpeed() { super(Stats.RUN_SPEED, 0x30, null, null); }

    @Override
    public void calc(Env env) {
        env.mulValue(Formulas.DEXbonus[env.getCharacter().getDEX()]);
    }
}