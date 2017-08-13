package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.AskJoinParty;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

/**
 * format cdd
 */
public final class RequestJoinParty extends L2GameClientPacket {
    private String name;
    private int itemDistribution;

    private static void addTargetToParty(L2PcInstance target, L2PcInstance requestor) {
        L2Party party = requestor.getParty();
        if (party == null) { return; }

        if (!party.isLeader(requestor)) {
            requestor.sendPacket(SystemMessageId.ONLY_LEADER_CAN_INVITE);
            return;
        }

        if (party.getMemberCount() >= 9) {
            requestor.sendPacket(SystemMessageId.PARTY_FULL);
            return;
        }

        if (party.getPendingInvitation() && !party.isInvitationRequestExpired()) {
            requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
            return;
        }

        if (!target.isProcessingRequest()) {
            requestor.onTransactionRequest(target);

            // in case a leader change has happened, use party's mode
            target.sendPacket(new AskJoinParty(requestor.getName(), party.getLootDistribution()));
            party.setPendingInvitation(true);

            requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_INVITED_S1_TO_PARTY).addPcName(target));
        }
        else {
            requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addPcName(target));
        }
    }

    @Override
    protected void readImpl() {
        name = readS();
        itemDistribution = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance requestor = getClient().getActiveChar();
        if (requestor == null) { return; }

        L2PcInstance target = L2World.getInstance().getPlayer(name);
        if (target == null) {
            requestor.sendPacket(SystemMessageId.FIRST_SELECT_USER_TO_INVITE_TO_PARTY);
            return;
        }

        if (target.getContactController().isBlocked(requestor)) {
            requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addPcName(target));
            return;
        }

        if (target.equals(requestor) || target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped() || target.isInvisible()) {
            requestor.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
            return;
        }

        if (target.isInParty()) {
            requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_ALREADY_IN_PARTY).addPcName(target));
            return;
        }

        if (target.getClient().isDetached()) {
            requestor.sendMessage("The player you tried to invite is in offline mode.");
            return;
        }

        if (target.isInJail() || requestor.isInJail()) {
            requestor.sendMessage("The player you tried to invite is currently jailed.");
            return;
        }

        if (target.isInOlympiadMode() || requestor.isInOlympiadMode()) { return; }

        if (!requestor.isInParty()) { createNewParty(target, requestor); }
        else {
            if (!requestor.getParty().isInDimensionalRift()) { addTargetToParty(target, requestor); }
        }
    }

    private void createNewParty(L2PcInstance target, L2PcInstance requestor) {
        if (!target.isProcessingRequest()) {
            requestor.setParty(new L2Party(requestor, itemDistribution));

            requestor.onTransactionRequest(target);
            target.sendPacket(new AskJoinParty(requestor.getName(), itemDistribution));
            requestor.getParty().setPendingInvitation(true);
            requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_INVITED_S1_TO_PARTY).addPcName(target));
        }
        else {
            requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
        }
    }
}