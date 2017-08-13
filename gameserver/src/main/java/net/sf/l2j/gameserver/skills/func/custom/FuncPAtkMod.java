package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncPAtkMod extends Func {
    public FuncPAtkMod() { super(Stats.POWER_ATTACK, 0x30, null, null); }

    @Override
    public void calc(Env env) {
        if (env.getCharacter() instanceof L2PetInstance) { return; }
        env.mulValue(Formulas.STRbonus[env.getCharacter().getSTR()] * env.getCharacter().getLevelMod());
    }
}