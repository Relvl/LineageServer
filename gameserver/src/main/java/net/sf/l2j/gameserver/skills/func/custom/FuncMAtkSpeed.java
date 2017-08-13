package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncMAtkSpeed extends Func {
    public FuncMAtkSpeed() { super(Stats.MAGIC_ATTACK_SPEED, 0x20, null, null); }

    @Override
    public void calc(Env env) {
        env.mulValue(Formulas.WITbonus[env.getCharacter().getWIT()]);
    }
}