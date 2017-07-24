package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;

public class SummonStatus extends PlayableStatus {
    public SummonStatus(L2Summon activeChar) {
        super(activeChar);
    }

    @Override
    public void reduceHp(double value, L2Character attacker) {
        reduceHp(value, attacker, true, false, false);
    }

    @Override
    public void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption) {
        if (getActiveChar().isDead()) { return; }

        if (attacker != null) {
            final L2PcInstance attackerPlayer = attacker.getActingPlayer();
            if (attackerPlayer != null && (getActiveChar().getOwner() == null || getActiveChar().getOwner().getDuelId() != attackerPlayer.getDuelId())) { attackerPlayer.setDuelState(DuelState.INTERRUPTED); }
        }
        super.reduceHp(value, attacker, awake, isDOT, isHPConsumption);
    }

    @Override
    public L2Summon getActiveChar() {
        return (L2Summon) super.getActiveChar();
    }
}