package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;

public class GuardKnownList extends AttackableKnownList {
    public GuardKnownList(L2GuardInstance activeChar) {
        super(activeChar);
    }

    @Override
    public boolean addKnownObject(L2Object object) {
        if (!super.addKnownObject(object)) { return false; }
        L2GuardInstance guard = (L2GuardInstance) this.object;
        if (object.isPlayer()) {
            if (object.getActingPlayer().getKarma() > 0) {
                if (guard.getAI().getIntention() == EIntention.IDLE) {
                    guard.getAI().setIntention(EIntention.ACTIVE, null);
                }
            }
        }
        else if (Config.GUARD_ATTACK_AGGRO_MOB && guard.isInActiveRegion() && object instanceof L2MonsterInstance) {
            if (((L2MonsterInstance) object).isAggressive()) {
                if (guard.getAI().getIntention() == EIntention.IDLE) {
                    guard.getAI().setIntention(EIntention.ACTIVE, null);
                }
            }
        }
        return true;
    }

    @Override
    public boolean removeKnownObject(L2Object object) {
        if (!super.removeKnownObject(object)) { return false; }
        L2GuardInstance guard = (L2GuardInstance) this.object;
        if (guard.gotNoTarget()) {
            if (guard.hasAI()) { guard.getAI().setIntention(EIntention.IDLE, null); }
        }
        return true;
    }
}