package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;

public class L2CastleTeleportZone extends L2ZoneType {
    private final int[] spawnLoc = new int[5];
    private int castleId;

    public L2CastleTeleportZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("castleId")) { castleId = Integer.parseInt(value); }
        else if (name.equals("spawnMinX")) { spawnLoc[0] = Integer.parseInt(value); }
        else if (name.equals("spawnMaxX")) { spawnLoc[1] = Integer.parseInt(value); }
        else if (name.equals("spawnMinY")) { spawnLoc[2] = Integer.parseInt(value); }
        else if (name.equals("spawnMaxY")) { spawnLoc[3] = Integer.parseInt(value); }
        else if (name.equals("spawnZ")) { spawnLoc[4] = Integer.parseInt(value); }
        else { super.setParameter(name, value); }
    }

    @Override
    protected void onEnter(L2Character character) {
        character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
    }

    @Override
    protected void onExit(L2Character character) {
        character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }

    public void oustAllPlayers() {
        if (_characterList.isEmpty()) { return; }

        for (L2PcInstance player : getKnownTypeInside(L2PcInstance.class)) {
            if (player.isOnline()) { player.teleToLocation(Rnd.get(spawnLoc[0], spawnLoc[1]), Rnd.get(spawnLoc[2], spawnLoc[3]), spawnLoc[4], 0); }
        }
    }

    public int getCastleId() {
        return castleId;
    }

    public int[] getSpawn() {
        return spawnLoc;
    }
}