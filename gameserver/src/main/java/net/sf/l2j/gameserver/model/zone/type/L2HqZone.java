package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;

public class L2HqZone extends L2ZoneType {

    public L2HqZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
        if (character.isPlayer()) {
            character.setInsideZone(ZoneId.HQ, true);
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character.isPlayer()) {
            character.setInsideZone(ZoneId.HQ, false);
        }
    }

    @Override
    public void onDieInside(L2Character character) { }

    @Override
    public void onReviveInside(L2Character character) { }
}