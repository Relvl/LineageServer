package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncHennaMEN extends Func {
    public FuncHennaMEN() { super(Stats.STAT_MEN, 0x10, null, null); }

    @Override
    public void calc(Env env) {
        if (env.getPlayer() != null) {
            env.addValue(env.getPlayer().getHennaStatMEN());
        }
    }
}