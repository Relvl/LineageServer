package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;
import net.sf.l2j.gameserver.model.entity.Castle;

public class SiegeGuardKnownList extends AttackableKnownList {
    public SiegeGuardKnownList(L2SiegeGuardInstance activeChar) {
        super(activeChar);
    }

    @Override
    public boolean addKnownObject(L2Object object) {
        if (!super.addKnownObject(object)) { return false; }
        L2SiegeGuardInstance guard = (L2SiegeGuardInstance) this.object;
        Castle castle = guard.getCastle();
        if (castle != null && castle.getZone().isActive()) {
            L2PcInstance player = object.getActingPlayer();
            if (player != null && (player.getClan() == null || castle.getSiege().getAttackerClan(player.getClan()) != null)) {
                if (guard.getAI().getIntention() == EIntention.IDLE) { guard.getAI().setIntention(EIntention.ACTIVE, null); }
            }
        }
        return true;
    }
}