package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class PartyInfo implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = { 81 };

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        if (!activeChar.isInParty()) { return false; }

        L2Party playerParty = activeChar.getParty();
        int memberCount = playerParty.getMemberCount();
        int lootDistribution = playerParty.getLootDistribution();
        String partyLeader = playerParty.getLeader().getName();

        activeChar.sendPacket(SystemMessageId.PARTY_INFORMATION);

        switch (lootDistribution) {
            case L2Party.ITEM_LOOTER:
                activeChar.sendPacket(SystemMessageId.LOOTING_FINDERS_KEEPERS);
                break;
            case L2Party.ITEM_ORDER:
                activeChar.sendPacket(SystemMessageId.LOOTING_BY_TURN);
                break;
            case L2Party.ITEM_ORDER_SPOIL:
                activeChar.sendPacket(SystemMessageId.LOOTING_BY_TURN_INCLUDE_SPOIL);
                break;
            case L2Party.ITEM_RANDOM:
                activeChar.sendPacket(SystemMessageId.LOOTING_RANDOM);
                break;
            case L2Party.ITEM_RANDOM_SPOIL:
                activeChar.sendPacket(SystemMessageId.LOOTING_RANDOM_INCLUDE_SPOIL);
                break;
        }
        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PARTY_LEADER_S1).addString(partyLeader));
        activeChar.sendMessage("Members: " + memberCount + "/9");
        activeChar.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
        return true;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}