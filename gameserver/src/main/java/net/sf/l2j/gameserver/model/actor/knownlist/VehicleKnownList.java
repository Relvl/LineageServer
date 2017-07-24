package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class VehicleKnownList extends CharKnownList {
    public VehicleKnownList(L2Character activeChar) {
        super(activeChar);
    }

    @Override
    public int getDistanceToWatchObject(L2Object object) {
        if (!(object instanceof L2PcInstance)) { return 0; }
        return object.getKnownList().getDistanceToWatchObject(this.object);
    }

    @Override
    public int getDistanceToForgetObject(L2Object object) {
        if (!(object instanceof L2PcInstance)) { return 0; }
        return object.getKnownList().getDistanceToForgetObject(this.object);
    }
}