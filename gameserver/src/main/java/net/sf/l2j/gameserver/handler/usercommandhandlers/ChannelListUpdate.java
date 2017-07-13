package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2CommandChannel;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.ExMultiPartyCommandChannelInfo;

public class ChannelListUpdate implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = {97};

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        if (!activeChar.isInParty()) { return false; }
        L2CommandChannel channel = activeChar.getParty().getCommandChannel();
        if (channel == null) { return false; }
        activeChar.sendPacket(new ExMultiPartyCommandChannelInfo(channel));
        return true;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}