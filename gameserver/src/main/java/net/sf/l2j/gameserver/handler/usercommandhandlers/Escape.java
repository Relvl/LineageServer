package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound;

public class Escape implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = {52};

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        if (activeChar.isCastingNow() || activeChar.isSitting() || activeChar.isMovementDisabled() || activeChar.isOutOfControl() || activeChar.isInOlympiadMode() || activeChar.inObserverMode() || activeChar.isFestivalParticipant() || activeChar.isInJail() || GrandBossManager.getInstance().isInBossZone(activeChar)) {
            activeChar.sendMessage("Your current state doesn't allow you to use the /unstuck command.");
            return false;
        }

        activeChar.stopMove(null);

        // Official timer 5 minutes, for GM 1 second
        if (activeChar.isGM()) {
            activeChar.doCast(SkillTable.getInstance().getInfo(2100, 1));
        }
        else {
            activeChar.sendPacket(new PlaySound("systemmsg_e.809"));
            activeChar.sendPacket(SystemMessageId.STUCK_TRANSPORT_IN_FIVE_MINUTES);
            activeChar.doCast(SkillTable.getInstance().getInfo(2099, 1));
        }

        return true;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}