package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class DisMount implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = { 62 };

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        if (activeChar.isMounted()) { activeChar.dismount(); }
        return true;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}