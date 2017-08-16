package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles following admin commands: - heal = restores HP/MP/CP on target, name or radius
 */
public class AdminHeal implements IAdminCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminHeal.class);
    private static final String[] ADMIN_COMMANDS = { "admin_heal" };

    private static void handleRes(L2PcInstance activeChar) {
        handleRes(activeChar, null);
    }

    private static void handleRes(L2PcInstance activeChar, String player) {
        L2Object obj = activeChar.getTarget();
        if (player != null) {
            L2PcInstance plyr = L2World.getInstance().getPlayer(player);

            if (plyr != null) { obj = plyr; }
            else {
                try {
                    int radius = Integer.parseInt(player);
                    for (L2Character character : activeChar.getKnownList().getKnownType(L2Character.class)) {
                        character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
                        if (character instanceof L2PcInstance) { character.setCurrentCp(character.getMaxCp()); }
                    }
                    activeChar.sendMessage("Healed within " + radius + " unit radius.");
                    return;
                }
                catch (NumberFormatException nbe) {
                }
            }
        }

        if (obj == null) { obj = activeChar; }

        if (obj instanceof L2Character) {
            L2Character target = (L2Character) obj;
            target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());

            if (target instanceof L2PcInstance) { target.setCurrentCp(target.getMaxCp()); }

            LOGGER.info("GM: {}({}) healed character {}", activeChar.getName(), activeChar.getObjectId(), target.getName());
        }
        else { activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET); }
    }

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (command.equals("admin_heal")) { handleRes(activeChar); }
        else if (command.startsWith("admin_heal")) {
            try {
                String healTarget = command.substring(11);
                handleRes(activeChar, healTarget);
            }
            catch (StringIndexOutOfBoundsException e) {
                activeChar.sendMessage("Incorrect target/radius specified.");
            }
        }
        return true;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}