package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;

public class L2NoRestartZone extends L2ZoneType {
    public L2NoRestartZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
        if (character instanceof L2PcInstance) {
            character.setInsideZone(ZoneId.NO_RESTART, true);
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character instanceof L2PcInstance) {
            character.setInsideZone(ZoneId.NO_RESTART, false);
        }
    }

    @Override
    public void onDieInside(L2Character character) { }

    @Override
    public void onReviveInside(L2Character character) { }
}