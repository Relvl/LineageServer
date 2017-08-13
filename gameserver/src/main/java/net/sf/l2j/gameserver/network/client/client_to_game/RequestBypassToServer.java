package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;
import net.sf.l2j.gameserver.util.GMAudit;

import java.util.StringTokenizer;

public final class RequestBypassToServer extends L2GameClientPacket {
    private String command;

    private static void playerHelp(L2PcInstance activeChar, String path) {
        if (path.indexOf("..") != -1) { return; }

        StringTokenizer st = new StringTokenizer(path);
        String[] cmd = st.nextToken().split("#");

        NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/help/" + cmd[0]);
        if (cmd.length > 1) { html.setItemId(Integer.parseInt(cmd[1])); }
        html.disableValidation();
        activeChar.sendPacket(html);
    }

    @Override
    protected void readImpl() {
        command = readS();
    }

    @Override
    protected void runImpl() {
        if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS)) { return; }

        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (command.isEmpty()) {
            _log.info("{} sent an empty requestBypass packet.", activeChar.getName());
            activeChar.logout();
            return;
        }

        System.out.println(">>> RequestBypassToServer: " + command);

        try {
            if (command.startsWith("admin_")) {
                String command = this.command.split(" ")[0];

                IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);
                if (ach == null) {
                    if (activeChar.isGM()) {
                        activeChar.sendMessage("The command " + command.substring(6) + " doesn't exist.");
                    }

                    _log.warn("No handler registered for admin command '{}'", command);
                    return;
                }

                if (!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel())) {
                    activeChar.sendMessage("You don't have the access rights to use this command.");
                    _log.warn("{} tried to use admin command {} without proper Access Level.", activeChar.getName(), command);
                    return;
                }

                if (Config.GMAUDIT) {
                    GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", this.command, activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target");
                }

                ach.useAdminCommand(this.command, activeChar);
            }
            else if (command.startsWith("player_help ")) {
                playerHelp(activeChar, command.substring(12));
            }
            else if (command.startsWith("npc_")) {
                if (!activeChar.validateBypass(command)) { return; }

                int endOfId = command.indexOf('_', 5);
                String id;
                if (endOfId > 0) { id = command.substring(4, endOfId); }
                else { id = command.substring(4); }

                try {
                    L2Object object = L2World.getInstance().getObject(Integer.parseInt(id));

                    if (object != null && object instanceof L2Npc && endOfId > 0 && ((L2Npc) object).canInteract(activeChar)) {
                        ((L2Npc) object).onBypassFeedback(activeChar, command.substring(endOfId + 1));
                    }

                    activeChar.sendPacket(ActionFailed.STATIC_PACKET);
                }
                catch (NumberFormatException nfe) {
                }
            }
            // Navigate throught Manor windows
            else if (command.startsWith("manor_menu_select?")) {
                L2Object object = activeChar.getTarget();
                if (object instanceof L2Npc) { ((L2Npc) object).onBypassFeedback(activeChar, command); }
            }
            else if (command.startsWith("bbs_") || command.startsWith("_bbs") || command.startsWith("_friend") || command.startsWith("_mail") || command.startsWith("_block")) {
                CommunityBoard.handleCommand(getClient().getActiveChar(), command);
            }
            else if (command.startsWith("Quest ")) {
                if (!activeChar.validateBypass(command)) { return; }

                String[] str = command.substring(6).trim().split(" ", 2);
                if (str.length == 1) { activeChar.processQuestEvent(str[0], ""); }
                else { activeChar.processQuestEvent(str[0], str[1]); }
            }
            else if (command.startsWith("_match")) {
                String params = command.substring(command.indexOf("?") + 1);
                StringTokenizer st = new StringTokenizer(params, "&");
                int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
                int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
                int heroid = Hero.getInstance().getHeroByClass(heroclass);
                if (heroid > 0) { Hero.getInstance().showHeroFights(activeChar, heroclass, heroid, heropage); }
            }
            else if (command.startsWith("_diary")) {
                String params = command.substring(command.indexOf("?") + 1);
                StringTokenizer st = new StringTokenizer(params, "&");
                int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
                int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
                int heroid = Hero.getInstance().getHeroByClass(heroclass);
                if (heroid > 0) { Hero.getInstance().showHeroDiary(activeChar, heroclass, heroid, heropage); }
            }
            else if (command.startsWith("arenachange")) {
                boolean isManager = activeChar.getCurrentFolkNPC() instanceof L2OlympiadManagerInstance;
                if (!isManager) {
                    // Without npc, command can be used only in observer mode on arena
                    if (!activeChar.isInObserverMode() || activeChar.isInOlympiadMode() || activeChar.getOlympiadGameId() < 0) {
                        return;
                    }
                }

                if (OlympiadManager.getInstance().isRegisteredInComp(activeChar)) {
                    activeChar.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
                    return;
                }

                int arenaId = Integer.parseInt(command.substring(12).trim());
                activeChar.enterOlympiadObserverMode(arenaId);
            }
        }
        catch (Exception e) {
            _log.error("Bad RequestBypassToServer: ", e);
        }
    }
}