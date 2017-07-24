package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;

public class AttackableStatus extends NpcStatus {
    public AttackableStatus(L2Attackable activeChar) {
        super(activeChar);
    }

    @Override
    public final void reduceHp(double value, L2Character attacker) {
        reduceHp(value, attacker, true, false, false);
    }

    @Override
    public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHpConsumption) {
        if (getActiveChar().isDead()) { return; }
        if (value > 0) {
            if (getActiveChar().isOverhit()) { getActiveChar().setOverhitValues(attacker, value); }
            else { getActiveChar().overhitEnabled(false); }
        }
        else { getActiveChar().overhitEnabled(false); }
        if (attacker != null) { getActiveChar().addAttackerToAttackByList(attacker); }
        super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
        if (!getActiveChar().isDead()) { getActiveChar().overhitEnabled(false); }
    }

    @Override
    public L2Attackable getActiveChar() {
        return (L2Attackable) super.getActiveChar();
    }
}