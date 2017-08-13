package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.datatables.*;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;

import java.util.StringTokenizer;

public class AdminAdmin implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
            {
                    "admin_admin",
                    "admin_admin1",
                    "admin_admin2",
                    "admin_admin3",
                    "admin_admin4",
                    "admin_gmlist",
                    "admin_kill",
                    "admin_silence",
                    "admin_tradeoff",
                    "admin_reload"
            };

    private static void kill(L2PcInstance activeChar, L2Character target) {
        if (target instanceof L2PcInstance) {
            if (!((L2PcInstance) target).isGM()) {
                target.stopAllEffects(); // e.g. invincibility effect
            }
            target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
        }
        else if (target.isChampion()) {
            target.reduceCurrentHp(target.getMaxHp() * Config.CHAMPION_HP + 1, activeChar, null);
        }
        else { target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null); }
    }

    private static void showMainPage(L2PcInstance activeChar, String command) {
        int mode = 0;
        String filename = null;
        try {
            mode = Integer.parseInt(command.substring(11));
        }
        catch (Exception e) {
        }

        switch (mode) {
            case 1:
                filename = "main";
                break;
            case 2:
                filename = "game";
                break;
            case 3:
                filename = "effects";
                break;
            case 4:
                filename = "server";
                break;
            default:
                filename = "main";
                break;
        }
        AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");
    }

    @Override
    public boolean useAdminCommand(String command, L2PcInstance player) {
        if (command.startsWith("admin_admin")) { showMainPage(player, command); }
        else if (command.startsWith("admin_gmlist")) {
            final boolean visibleStatus = GmListTable.getInstance().isGmVisible(player);

            GmListTable.getInstance().showOrHideGm(player, !visibleStatus);
            player.sendMessage((visibleStatus) ? "Registered into GMList." : "Removed from GMList.");
        }
        else if (command.startsWith("admin_kill")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            st.nextToken(); // skip command

            if (!st.hasMoreTokens()) {
                final L2Object obj = player.getTarget();
                if (!(obj instanceof L2Character)) { player.sendPacket(SystemMessageId.INCORRECT_TARGET); }
                else { kill(player, (L2Character) obj); }

                return true;
            }

            String firstParam = st.nextToken();
            L2PcInstance targetPlayer = L2World.getInstance().getPlayer(firstParam);
            if (targetPlayer != null) {
                if (st.hasMoreTokens()) {
                    String secondParam = st.nextToken();
                    if (StringUtil.isDigit(secondParam)) {
                        int radius = Integer.parseInt(secondParam);
                        for (L2Character knownChar : targetPlayer.getKnownList().getKnownTypeInRadius(L2Character.class, radius)) {
                            if (knownChar.equals(player)) { continue; }

                            kill(player, knownChar);
                        }
                        player.sendMessage("Killed all characters within a " + radius + " unit radius around " + targetPlayer.getName() + ".");
                    }
                    else { player.sendMessage("Invalid radius."); }
                }
                else { kill(player, targetPlayer); }
            }
            else if (StringUtil.isDigit(firstParam)) {
                int radius = Integer.parseInt(firstParam);
                for (L2Character knownChar : player.getKnownList().getKnownTypeInRadius(L2Character.class, radius)) {
                    if (knownChar.equals(player)) { continue; }

                    kill(player, knownChar);
                }
                player.sendMessage("Killed all characters within a " + radius + " unit radius.");
            }
        }
        else if (command.startsWith("admin_silence")) {
            if (player.getContactController().isBlockAll()) // already in message refusal mode
            {
                player.getContactController().setBlockAll(false);
                player.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
            }
            else {
                player.getContactController().setBlockAll(true);
                player.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
            }
        }
        else if (command.startsWith("admin_tradeoff")) {
            try {
                String mode = command.substring(15);
                if (mode.equalsIgnoreCase("on")) {
                    player.getContactController().setTradeRefusal(true);
                    player.sendMessage("Trade refusal enabled");
                }
                else if (mode.equalsIgnoreCase("off")) {
                    player.getContactController().setTradeRefusal(false);
                    player.sendMessage("Trade refusal disabled");
                }
            }
            catch (RuntimeException e) {
                if (player.getContactController().isTradeRefusal()) {
                    player.getContactController().setTradeRefusal(false);
                    player.sendMessage("Trade refusal disabled");
                }
                else {
                    player.getContactController().setTradeRefusal(true);
                    player.sendMessage("Trade refusal enabled");
                }
            }
        }
        else if (command.startsWith("admin_reload")) {
            StringTokenizer st = new StringTokenizer(command);
            st.nextToken();
            try {
                String type = st.nextToken();
                if (type.startsWith("acar")) {
                    AdminCommandAccessRights.getInstance().reload();
                    player.sendMessage("Admin commands rights have been reloaded.");
                }
                else if (type.startsWith("announcement")) {
                    AnnouncementTable.getInstance().reload();
                    player.sendMessage("The content of announcements.xml has been reloaded.");
                }
                else if (type.startsWith("config")) {
                    Config.load();
                    player.sendMessage("Configs files have been reloaded.");
                }
                else if (type.startsWith("crest")) {
                    CrestCache.getInstance().reload();
                    player.sendMessage("Crests have been reloaded.");
                }
                else if (type.startsWith("cw")) {
                    CursedWeaponsManager.getInstance().reload();
                    player.sendMessage("Cursed weapons have been reloaded.");
                }
                else if (type.startsWith("door")) {
                    DoorTable.getInstance().reload();
                    player.sendMessage("Doors instance has been reloaded.");
                }
                else if (type.startsWith("item")) {
                    ItemTable.getInstance().reload();
                    player.sendMessage("Items' templates have been reloaded.");
                }
                else if (type.equals("multisell")) {
                    MultisellData.getInstance().reload();
                    player.sendMessage("The multisell instance has been reloaded.");
                }
                else if (type.equals("npc")) {
                    NpcTable.getInstance().reloadAllNpc();
                    player.sendMessage("NPCs templates have been reloaded.");
                }
                else if (type.startsWith("npcwalker")) {
                    NpcWalkerRoutesTable.getInstance().reload();
                    player.sendMessage("NPCwalkers' routes have been reloaded.");
                }
                else if (type.startsWith("skill")) {
                    SkillTable.getInstance().reload();
                    player.sendMessage("Skills' XMLs have been reloaded.");
                }
                else if (type.startsWith("teleport")) {
                    TeleportLocationTable.getInstance().reload();
                    player.sendMessage("The teleport location table has been reloaded.");
                }
                else if (type.startsWith("zone")) {
                    ZoneManager.getInstance().reload();
                    player.sendMessage("Zones have been reloaded.");
                }
            }
            catch (Exception e) {
                player.sendMessage("Usage : //reload <acar|announcement|config|crest|door>");
                player.sendMessage("Usage : //reload <htm|item|multisell|npc|npcwalker>");
                player.sendMessage("Usage : //reload <skill|teleport|zone>");
            }
        }
        return true;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}