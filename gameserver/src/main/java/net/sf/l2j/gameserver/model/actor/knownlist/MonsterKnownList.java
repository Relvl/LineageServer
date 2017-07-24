package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class MonsterKnownList extends AttackableKnownList {
    public MonsterKnownList(L2MonsterInstance activeChar) {
        super(activeChar);
    }

    @Override
    public boolean addKnownObject(L2Object object) {
        if (!super.addKnownObject(object)) { return false; }
        if (object.isPlayer()) {
            L2CharacterAI ai = ((L2MonsterInstance) this.object).getAI();
            if (ai != null && ai.getIntention() == EIntention.IDLE) {
                ai.setIntention(EIntention.ACTIVE, null);
            }
        }
        return true;
    }

    @Override
    public boolean removeKnownObject(L2Object object) {
        if (!super.removeKnownObject(object)) { return false; }
        if (!(object instanceof L2Character)) { return true; }
        L2MonsterInstance monster = (L2MonsterInstance) this.object;
        if (monster.hasAI()) { monster.getAI().notifyEvent(ECtrlEvent.EVT_FORGET_OBJECT, object); }
        if (monster.isVisible() && getKnownType(L2PcInstance.class).isEmpty()) { monster.clearAggroList(); }
        return true;
    }
}