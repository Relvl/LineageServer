package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public final class RequestPetition extends L2GameClientPacket {
    private String _content;
    private int _type; // 1 = on : 0 = off;

    @Override
    protected void readImpl() {
        _content = readS();
        _type = readD();
    }

    @Override
    protected void runImpl() {
        final L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (!GmListTable.getInstance().isGmOnline(false)) {
            activeChar.sendPacket(SystemMessageId.NO_GM_PROVIDING_SERVICE_NOW);
            activeChar.sendPacket(new PlaySound("systemmsg_e.702"));
            return;
        }

        if (!PetitionManager.isPetitioningAllowed()) {
            activeChar.sendPacket(SystemMessageId.GAME_CLIENT_UNABLE_TO_CONNECT_TO_PETITION_SERVER);
            return;
        }

        if (PetitionManager.getInstance().isPlayerPetitionPending(activeChar)) {
            activeChar.sendPacket(SystemMessageId.ONLY_ONE_ACTIVE_PETITION_AT_TIME);
            return;
        }

        if (PetitionManager.getInstance().getPendingPetitionCount() == Config.MAX_PETITIONS_PENDING) {
            activeChar.sendPacket(SystemMessageId.PETITION_SYSTEM_CURRENT_UNAVAILABLE);
            return;
        }

        int totalPetitions = PetitionManager.getInstance().getPlayerTotalPetitionCount(activeChar) + 1;
        if (totalPetitions > Config.MAX_PETITIONS_PER_PLAYER) {
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WE_HAVE_RECEIVED_S1_PETITIONS_TODAY).addNumber(totalPetitions));
            return;
        }

        if (_content.length() > 255) {
            activeChar.sendPacket(SystemMessageId.PETITION_MAX_CHARS_255);
            return;
        }

        int petitionId = PetitionManager.getInstance().submitPetition(activeChar, _content, _type);

        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PETITION_ACCEPTED_RECENT_NO_S1).addNumber(petitionId));
        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SUBMITTED_YOU_S1_TH_PETITION_S2_LEFT).addNumber(totalPetitions).addNumber(Config.MAX_PETITIONS_PER_PLAYER - totalPetitions));
        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PETITION_ON_WAITING_LIST).addNumber(PetitionManager.getInstance().getPendingPetitionCount()));
    }
}