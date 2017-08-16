package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncAtkCritical extends Func {
    public FuncAtkCritical() { super(Stats.CRITICAL_RATE, 0x09, null, null); }

    @Override
    public void calc(Env env) {
        if (!env.getCharacter().isSummon()) {
            env.mulValue(Formulas.DEXbonus[env.getCharacter().getDEX()]);
        }
        env.mulValue(10);
        env.setBaseValue(env.getValue());
    }
}
