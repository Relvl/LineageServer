package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

public class L2JailZone extends L2ZoneType {
    public L2JailZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
        if (character.isPlayer()) {
            character.setInsideZone(ZoneId.JAIL, true);
            character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
            character.setInsideZone(ZoneId.NO_STORE, true);
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character.isPlayer()) {
            character.setInsideZone(ZoneId.JAIL, false);
            character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
            character.setInsideZone(ZoneId.NO_STORE, false);

            L2PcInstance player = (L2PcInstance) character;
            if (player.isInJail() && !player.isInsideZone(ZoneId.JAIL)) {
                // when a player wants to exit jail even if he is still jailed, teleport him back to jail
                ThreadPoolManager.getInstance().schedule(new BackToJail(player), 2000);
                player.sendMessage("You cannot cheat your way out of here. You must wait until your jail time is over.");
            }
        }
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }

    private static class BackToJail implements Runnable {
        private final L2PcInstance player;

        BackToJail(L2PcInstance character) {
            player = character;
        }

        @Override
        public void run() {
            player.teleToLocation(-114356, -249645, -2984, 0);
        }
    }
}