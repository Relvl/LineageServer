package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

public final class Logout extends L2GameClientPacket {
    @Override
    protected void readImpl() {
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        if (player.getActiveEnchantItem() != null || player.isSubclassChangeLocked()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (player.isInsideZone(ZoneId.NO_RESTART)) {
            player.sendPacket(SystemMessageId.NO_LOGOUT_HERE);
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (AttackStanceTaskManager.getInstance().isInAttackStance(player)) {
            player.sendPacket(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING);
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (player.isFestivalParticipant()) {
            if (SevenSignsFestival.getInstance().isFestivalInitialized()) {
                player.sendPacket(SystemMessageId.NO_LOGOUT_HERE);
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }

            if (player.isInParty()) {
                player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming festival."));
            }
        }

        player.removeFromBossZone();
        player.logout();
    }
}