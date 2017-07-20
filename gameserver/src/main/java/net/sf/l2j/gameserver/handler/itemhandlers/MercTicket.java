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
        L2PcInstance activeChar = (L2PcInstance) playable;
        if (activeChar == null) { return; }

        Castle castle = CastleManager.getInstance().getCastle(activeChar);
        if (castle == null) { return; }

        int castleId = castle.getCastleId();
        int itemId = item.getItemId();

        // add check that certain tickets can only be placed in certain castles
        if (MercTicketManager.getTicketCastleId(itemId) != castleId) {
            activeChar.sendPacket(SystemMessageId.MERCENARIES_CANNOT_BE_POSITIONED_HERE);
            return;
        }

        if (!activeChar.isCastleLord(castleId)) {
            activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_POSITION_MERCENARIES);
            return;
        }

        if (castle.getSiege().isInProgress()) {
            activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
            return;
        }

        // Checking Seven Signs Quest Period
        if (SevenSigns.getInstance().getCurrentPeriod() != SevenSigns.PERIOD_SEAL_VALIDATION) {
            activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
            return;
        }

        // Checking the Seal of Strife status
        switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE)) {
            case SevenSigns.CABAL_NULL:
                if (SevenSigns.getInstance().checkIsDawnPostingTicket(itemId)) {
                    activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
                    return;
                }
                break;

            case SevenSigns.CABAL_DUSK:
                if (!SevenSigns.getInstance().checkIsRookiePostingTicket(itemId)) {
                    activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
                    return;
                }
                break;
        }

        if (MercTicketManager.getInstance().isAtCasleLimit(item.getItemId())) {
            activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
            return;
        }

        if (MercTicketManager.getInstance().isAtTypeLimit(item.getItemId())) {
            activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
            return;
        }

        if (MercTicketManager.getInstance().isTooCloseToAnotherTicket(activeChar.getX(), activeChar.getY(), activeChar.getZ())) {
            activeChar.sendPacket(SystemMessageId.POSITIONING_CANNOT_BE_DONE_BECAUSE_DISTANCE_BETWEEN_MERCENARIES_TOO_SHORT);
            return;
        }

        MercTicketManager.getInstance().addTicket(item.getItemId(), activeChar);
        activeChar.destroyItem(EItemProcessPurpose.CONSUME, item.getObjectId(), 1, null, false); // Remove item from char's inventory
        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PLACE_S1_IN_CURRENT_LOCATION_AND_DIRECTION).addItemName(itemId));
    }
}