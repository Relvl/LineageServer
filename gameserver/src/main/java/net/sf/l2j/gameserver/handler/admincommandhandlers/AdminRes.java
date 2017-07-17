package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminRes implements IAdminCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminRes.class);
    private static final String[] ADMIN_COMMANDS = {"admin_res", "admin_res_monster"};

    private static void handleRes(L2PcInstance activeChar) {
        handleRes(activeChar, null);
    }

    private static void handleRes(L2PcInstance activeChar, String resParam) {
        L2Object obj = activeChar.getTarget();

        if (resParam != null) {
            // Check if a player name was specified as a param.
            L2PcInstance plyr = L2World.getInstance().getPlayer(resParam);

            if (plyr != null) { obj = plyr; }
            else {
                // Otherwise, check if the param was a radius.
                try {
                    int radius = Integer.parseInt(resParam);

                    for (L2PcInstance knownPlayer : activeChar.getKnownList().getKnownTypeInRadius(L2PcInstance.class, radius)) { doResurrect(knownPlayer); }

                    activeChar.sendMessage("Resurrected all players within a " + radius + " unit radius.");
                    return;
                }
                catch (NumberFormatException e) {
                    activeChar.sendMessage("Enter a valid player name or radius.");
                    return;
                }
            }
        }

        if (obj == null) { obj = activeChar; }

        doResurrect((L2Character) obj);

        LOGGER.info("GM: {}({}) resurrected character {}", activeChar.getName(), activeChar.getObjectId(), obj.getObjectId());
    }

    private static void handleNonPlayerRes(L2PcInstance activeChar) {
        handleNonPlayerRes(activeChar, "");
    }

    private static void handleNonPlayerRes(L2PcInstance activeChar, String radiusStr) {
        L2Object obj = activeChar.getTarget();

        try {
            if (!radiusStr.isEmpty()) {
                int radius = Integer.parseInt(radiusStr);
                for (L2Character knownChar : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, radius)) { if (!(knownChar instanceof L2PcInstance)) { doResurrect(knownChar); } }
                activeChar.sendMessage("Resurrected all non-players within a " + radius + " unit radius.");
            }
        }
        catch (NumberFormatException e) {
            activeChar.sendMessage("Enter a valid radius.");
            return;
        }

        if (obj instanceof L2PcInstance) {
            activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
            return;
        }

        doResurrect((L2Character) obj);
    }

    private static void doResurrect(L2Character targetChar) {
        if (!targetChar.isDead()) { return; }

        // If the target is a player, then restore the XP lost on death.
        if (targetChar instanceof L2PcInstance) { ((L2PcInstance) targetChar).restoreExp(100.0); }
        // If the target is an NPC, then abort it's auto decay and respawn.
        else { DecayTaskManager.getInstance().cancel(targetChar); }

        targetChar.doRevive();
    }

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (command.startsWith("admin_res ")) { handleRes(activeChar, command.split(" ")[1]); }
        else if (command.equals("admin_res")) { handleRes(activeChar); }
        else if (command.startsWith("admin_res_monster ")) { handleNonPlayerRes(activeChar, command.split(" ")[1]); }
        else if (command.equals("admin_res_monster")) { handleNonPlayerRes(activeChar); }

        return true;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}