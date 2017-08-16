package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2CommandChannel;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class ChannelDelete implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = { 93 };

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        if (activeChar.isInParty()) {
            if (activeChar.getParty().isLeader(activeChar) && activeChar.getParty().isInCommandChannel() && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar)) {
                L2CommandChannel channel = activeChar.getParty().getCommandChannel();
                channel.broadcastToChannelMembers(SystemMessage.getSystemMessage(SystemMessageId.COMMAND_CHANNEL_DISBANDED));
                channel.disbandChannel();
                return true;
            }
        }
        return false;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}