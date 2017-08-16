package net.sf.l2j.gameserver.skills.func.custom;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.func.Func;

public class FuncMAtkCritical extends Func {
    public FuncMAtkCritical() { super(Stats.MCRITICAL_RATE, 0x30, null, null); }

    @Override
    public void calc(Env env) {
        L2Character player = env.getCharacter();
        if (player.isPlayer()) {
            if (player.getActiveWeaponInstance() != null) {
                env.mulValue(Formulas.WITbonus[player.getWIT()]);
            }
        }
        else {
            env.mulValue(Formulas.WITbonus[player.getWIT()]);
        }
    }
}