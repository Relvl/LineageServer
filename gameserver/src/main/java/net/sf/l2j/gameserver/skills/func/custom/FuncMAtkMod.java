package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncMAtkMod extends Func {
    public FuncMAtkMod() { super(Stats.MAGIC_ATTACK, 0x20, null, null); }

    @Override
    public void calc(Env env) {
        if (env.getCharacter() instanceof L2PetInstance) { return; }
        double intb = Formulas.INTbonus[env.getCharacter().getINT()];
        double lvlb = env.getCharacter().getLevelMod();
        env.mulValue(lvlb * lvlb * (intb * intb));
    }
}