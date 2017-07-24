package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.instancemanager.games.MonsterRace;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2RaceManagerInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.DeleteObject;

public class RaceManagerKnownList extends NpcKnownList {
    public RaceManagerKnownList(L2RaceManagerInstance activeChar) {
        super(activeChar);
    }

    @Override
    public boolean addKnownObject(L2Object object) {
        if (!super.addKnownObject(object)) { return false; }
        if (object.isPlayer()) { object.getActingPlayer().sendPacket(MonsterRace.getInstance().getRacePacket()); }
        return true;
    }

    @Override
    public boolean removeKnownObject(L2Object object) {
        if (!super.removeKnownObject(object)) { return false; }
        if (object.isPlayer()) {
            for (L2Npc npc : MonsterRace.getInstance().getMonsters()) {
                object.getActingPlayer().sendPacket(new DeleteObject(npc));
            }
        }
        return true;
    }
}