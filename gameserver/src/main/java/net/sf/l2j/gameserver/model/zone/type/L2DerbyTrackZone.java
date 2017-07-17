package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.zone.ZoneId;

public class L2DerbyTrackZone extends L2PeaceZone {
    public L2DerbyTrackZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
        if (character instanceof L2Playable) {
            character.setInsideZone(ZoneId.MONSTER_TRACK, true);
            character.setInsideZone(ZoneId.PEACE, true);
            character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character instanceof L2Playable) {
            character.setInsideZone(ZoneId.MONSTER_TRACK, false);
            character.setInsideZone(ZoneId.PEACE, false);
            character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
        }
    }

    @Override
    public void onDieInside(L2Character character) { }

    @Override
    public void onReviveInside(L2Character character) { }
}