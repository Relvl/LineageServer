package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncAtkAccuracy extends Func {
    public FuncAtkAccuracy() { super(Stats.ACCURACY_COMBAT, 0x10, null, null); }

    @Override
    public void calc(Env env) {
        int level = env.getCharacter().getLevel();
        env.addValue((Math.sqrt(env.getCharacter().getDEX()) * 6) + level);
        if (env.getCharacter().isSummon()) {
            env.addValue((level < 60) ? 4 : 5);
        }
    }
}