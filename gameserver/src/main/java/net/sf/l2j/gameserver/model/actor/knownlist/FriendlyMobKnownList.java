package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2FriendlyMobInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

@SuppressWarnings("ObjectEquality")
public class FriendlyMobKnownList extends AttackableKnownList {
    public FriendlyMobKnownList(L2FriendlyMobInstance activeChar) {
        super(activeChar);
    }

    @Override
    public boolean addKnownObject(L2Object object) {
        if (!super.addKnownObject(object)) { return false; }
        if (object.isPlayer()) {
            L2FriendlyMobInstance monster = (L2FriendlyMobInstance) this.object;
            if (monster.getAI().getIntention() == EIntention.IDLE) {
                monster.getAI().setIntention(EIntention.ACTIVE, null);
            }
        }

        return true;
    }

    @Override
    public boolean removeKnownObject(L2Object object) {
        if (!super.removeKnownObject(object)) { return false; }
        if (!(object instanceof L2Character)) { return true; }
        L2FriendlyMobInstance monster = (L2FriendlyMobInstance) this.object;
        if (monster.hasAI()) {
            monster.getAI().notifyEvent(ECtrlEvent.EVT_FORGET_OBJECT, object);
            if (monster.getTarget() == object) { monster.setTarget(null); }
        }
        if (monster.isVisible() && getKnownType(L2PcInstance.class).isEmpty()) {
            monster.clearAggroList();
            if (monster.hasAI()) { monster.getAI().setIntention(EIntention.IDLE, null); }
        }
        return true;
    }
}