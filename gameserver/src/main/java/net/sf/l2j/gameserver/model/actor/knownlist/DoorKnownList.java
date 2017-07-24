package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;

public class DoorKnownList extends CharKnownList {
    public DoorKnownList(L2DoorInstance activeChar) {
        super(activeChar);
    }

    @Override
    public int getDistanceToWatchObject(L2Object object) {
        if (object instanceof L2SiegeGuardInstance) { return 600; }
        if (object instanceof L2PcInstance) { return 2000; }
        return 0;
    }

    @Override
    public int getDistanceToForgetObject(L2Object object) {
        if (object instanceof L2SiegeGuardInstance) { return 900; }
        if (object instanceof L2PcInstance) { return 3000; }
        return 0;
    }
}