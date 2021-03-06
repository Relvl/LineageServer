package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class Time implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = { 77 };

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        int hour = GameTimeTaskManager.getInstance().getGameHour();
        int minute = GameTimeTaskManager.getInstance().getGameMinute();
        String min = ((minute < 10) ? "0" : "") + minute;

        activeChar.sendPacket(SystemMessage.getSystemMessage((GameTimeTaskManager.getInstance().isNight()) ? SystemMessageId.TIME_S1_S2_IN_THE_NIGHT : SystemMessageId.TIME_S1_S2_IN_THE_DAY).addNumber(hour).addString(min));
        return true;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}