package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Vehicle;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.DeleteObject;
import net.sf.l2j.gameserver.network.client.game_to_client.SpawnItemPoly;

public class PcKnownList extends PlayableKnownList {
    public PcKnownList(L2PcInstance activeChar) {
        super(activeChar);
    }

    @Override
    public boolean addKnownObject(L2Object object) {
        if (!super.addKnownObject(object)) { return false; }
        sendInfoFrom(object);
        return true;
    }

    @Override
    public boolean removeKnownObject(L2Object object) {
        if (!super.removeKnownObject(object)) { return false; }
        this.object.getActingPlayer().sendPacket(new DeleteObject(object, object.isPlayer() && object.getActingPlayer().isSeated()));
        return true;
    }

    @Override
    public int getDistanceToWatchObject(L2Object object) {
        if (object instanceof L2Vehicle) { return 8000; }
        return Math.max(1800, 3600 - (knownObjects.size() * 20));
    }

    @Override
    public int getDistanceToForgetObject(L2Object object) {
        return (int) Math.round(1.5 * getDistanceToWatchObject(object));
    }

    public final void refreshInfos() {
        for (L2Object kanownObject : knownObjects.values()) {
            if (kanownObject.isPlayer() && kanownObject.getActingPlayer().inObserverMode()) { continue; }
            sendInfoFrom(kanownObject);
        }
    }

    private void sendInfoFrom(L2Object object) {
        if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item")) {
            this.object.getActingPlayer().sendPacket(new SpawnItemPoly(object));
        }
        else {
            object.sendInfo(this.object.getActingPlayer());
            if (object instanceof L2Character) {
                L2Character obj = (L2Character) object;
                if (obj.hasAI()) {
                    obj.getAI().describeStateToPlayer(this.object.getActingPlayer());
                }
            }
        }
    }
}