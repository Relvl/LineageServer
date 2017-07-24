package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Summon;

@SuppressWarnings("ObjectEquality")
public class SummonKnownList extends PlayableKnownList {
    public SummonKnownList(L2Summon activeChar) {
        super(activeChar);
    }

    @Override
    public int getDistanceToWatchObject(L2Object object) { return 1500; }

    @Override
    public int getDistanceToForgetObject(L2Object object) {
        L2Summon summon = (L2Summon) this.object;
        if (object == summon.getOwner() || object == summon.getTarget()) { return 6000; }
        return 3000;
    }
}