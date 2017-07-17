package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class L2NoLandingZone extends L2ZoneType {
    private int dismountDelay = 5;

    public L2NoLandingZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("dismountDelay")) { dismountDelay = Integer.parseInt(value); }
        else { super.setParameter(name, value); }
    }

    @Override
    protected void onEnter(L2Character character) {
        if (character instanceof L2PcInstance) {
            character.setInsideZone(ZoneId.NO_LANDING, true);
            if (((L2PcInstance) character).getMountType() == 2) {
                character.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_WYVERN));
                ((L2PcInstance) character).enteredNoLanding(dismountDelay);
            }
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character instanceof L2PcInstance) {
            character.setInsideZone(ZoneId.NO_LANDING, false);
            if (((L2PcInstance) character).getMountType() == 2) { ((L2PcInstance) character).exitedNoLanding(); }
        }
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }
}