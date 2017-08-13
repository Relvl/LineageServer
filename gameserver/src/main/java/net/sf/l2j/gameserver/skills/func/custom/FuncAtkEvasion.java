package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncAtkEvasion extends Func {
    public FuncAtkEvasion() { super(Stats.EVASION_RATE, 0x10, null, null); }

    @Override
    public void calc(Env env) {
        env.addValue((Math.sqrt(env.getCharacter().getDEX()) * 6) + env.getCharacter().getLevel());
    }
}