package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.client.game_to_client.ClanHallDecoration;

public class L2ClanHallZone extends L2SpawnZone {
    private int clanHallId;

    public L2ClanHallZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("clanHallId")) {
            clanHallId = Integer.parseInt(value);
            ClanHallManager.getInstance().getClanHallById(clanHallId).setZone(this);
        }
        else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        if (character.isPlayer()) {
            character.setInsideZone(ZoneId.CLAN_HALL, true);

            ClanHall clanHall = ClanHallManager.getInstance().getClanHallById(clanHallId);
            if (clanHall == null) { return; }

            ClanHallDecoration deco = new ClanHallDecoration(clanHall);
            character.sendPacket(deco);
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character.isPlayer()) {
            character.setInsideZone(ZoneId.CLAN_HALL, false);
        }
    }

    @Override
    public void onDieInside(L2Character character) { }

    @Override
    public void onReviveInside(L2Character character) { }

    public void banishForeigners(int owningClanId) {
        for (L2PcInstance player : getKnownTypeInside(L2PcInstance.class)) {
            if (player.getClanId() == owningClanId) { continue; }

            player.teleToLocation(TeleportWhereType.Town);
        }
    }
}