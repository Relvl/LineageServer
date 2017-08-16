package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncHennaDEX extends Func {
    public FuncHennaDEX() { super(Stats.STAT_DEX, 0x10, null, null); }

    @Override
    public void calc(Env env) {
        if (env.getPlayer() != null) {
            env.addValue(env.getPlayer().getHennaStatDEX());
        }
    }
}