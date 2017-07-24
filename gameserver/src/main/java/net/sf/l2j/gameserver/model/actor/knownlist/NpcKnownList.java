package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2FestivalGuideInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;

public class NpcKnownList extends CharKnownList {
    public NpcKnownList(L2Npc activeChar) {
        super(activeChar);
    }

    @Override
    public int getDistanceToWatchObject(L2Object object) {
        if (object instanceof L2NpcInstance || !(object instanceof L2Character)) { return 0; }
        if (object instanceof L2Playable) {
            if (this.object instanceof L2FestivalGuideInstance) { return 4000; }
            return 1500;
        }
        return 500;
    }

    @Override
    public int getDistanceToForgetObject(L2Object object) {
        return (int) Math.round(1.5 * getDistanceToWatchObject(object));
    }
}