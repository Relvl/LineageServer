package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class Mount implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = { 61 };

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        return activeChar.mountPlayer(activeChar.getPet());
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}