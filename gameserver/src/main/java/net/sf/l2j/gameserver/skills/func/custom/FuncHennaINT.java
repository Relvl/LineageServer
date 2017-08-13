package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncHennaINT extends Func {
    public FuncHennaINT() { super(Stats.STAT_INT, 0x10, null, null); }

    @Override
    public void calc(Env env) {
        if (env.getPlayer() != null) {
            env.addValue(env.getPlayer().getHennaStatINT());
        }
    }
}