package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class MercTicket implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        L2PcInstance player = (L2PcInstance) playable;
        if (player == null) { return; }

        Castle castle = CastleManager.getInstance().getCastle(player);
        if (castle == null) { return; }

        int castleId = castle.getCastleId();
        int itemId = item.getItemId();

        // add check that certain tickets can only be placed in certain castles
        if (MercTicketManager.getTicketCastleId(itemId) != castleId) {
            player.sendPacket(SystemMessageId.MERCENARIES_CANNOT_BE_POSITIONED_HERE);
            return;
        }

        if (!player.isCastleLord(castleId)) {
            player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_POSITION_MERCENARIES);
            return;
        }

        if (castle.getSiege().isInProgress()) {
            player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
            return;
        }

        // Checking Seven Signs Quest Period
        if (SevenSigns.getInstance().getCurrentPeriod() != SevenSigns.PERIOD_SEAL_VALIDATION) {
            player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
            return;
        }

        // Checking the Seal of Strife status
        switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE)) {
            case SevenSigns.CABAL_NULL:
                if (SevenSigns.getInstance().checkIsDawnPostingTicket(itemId)) {
                    player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
                    return;
                }
                break;

            case SevenSigns.CABAL_DUSK:
                if (!SevenSigns.getInstance().checkIsRookiePostingTicket(itemId)) {
                    player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
                    return;
                }
                break;
        }

        if (MercTicketManager.getInstance().isAtCasleLimit(item.getItemId())) {
            player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
            return;
        }

        if (MercTicketManager.getInstance().isAtTypeLimit(item.getItemId())) {
            player.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
            return;
        }

        if (MercTicketManager.getInstance().isTooCloseToAnotherTicket(player.getX(), player.getY(), player.getZ())) {
            player.sendPacket(SystemMessageId.POSITIONING_CANNOT_BE_DONE_BECAUSE_DISTANCE_BETWEEN_MERCENARIES_TOO_SHORT);
            return;
        }

        MercTicketManager.getInstance().addTicket(item.getItemId(), player);
        player.getInventory().destroyItem(EItemProcessPurpose.CONSUME, item, 1, null, false); // Remove item from char's inventory
        player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PLACE_S1_IN_CURRENT_LOCATION_AND_DIRECTION).addItemName(itemId));
    }
}