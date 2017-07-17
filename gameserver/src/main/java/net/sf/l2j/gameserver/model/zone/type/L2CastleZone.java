package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.ZoneId;

public class L2CastleZone extends L2SpawnZone {
    private int castleId;
    private Castle castle;

    public L2CastleZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("castleId")) {
            castleId = Integer.parseInt(value);
        }
        else { super.setParameter(name, value); }
    }

    @Override
    protected void onEnter(L2Character character) {
        if (getCastle() != null) { character.setInsideZone(ZoneId.CASTLE, true); }
    }

    @Override
    protected void onExit(L2Character character) {
        if (getCastle() != null) { character.setInsideZone(ZoneId.CASTLE, false); }
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }

    public void banishForeigners(int owningClanId) {
        for (L2PcInstance player : getKnownTypeInside(L2PcInstance.class)) {
            if (player.getClanId() == owningClanId) { continue; }

            player.teleToLocation(TeleportWhereType.Town);
        }
    }

    public int getCastleId() {
        return castleId;
    }

    private Castle getCastle() {
        if (castle == null) {
            castle = CastleManager.getInstance().getCastleById(castleId);
        }
        return castle;
    }
}