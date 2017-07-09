/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;
import net.sf.l2j.gameserver.util.GMAudit;

import java.util.StringTokenizer;
import java.util.logging.Level;

public final class RequestBypassToServer extends L2GameClientPacket {
    private String _command;

    @Override
    protected void readImpl() {
        _command = readS();
    }

    @Override
    protected void runImpl() {
        if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS)) { return; }

        final L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (_command.isEmpty()) {
            _log.info(activeChar.getName() + " sent an empty requestBypass packet.");
            activeChar.logout();
            return;
        }

        try {
            if (_command.startsWith("admin_")) {
                String command = _command.split(" ")[0];

                IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);
                if (ach == null) {
                    if (activeChar.isGM()) {
                        activeChar.sendMessage("The command " + command.substring(6) + " doesn't exist.");
                    }

                    _log.warning("No handler registered for admin command '" + command + "'");
                    return;
                }

                if (!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel())) {
                    activeChar.sendMessage("You don't have the access rights to use this command.");
                    _log.warning(activeChar.getName() + " tried to use admin command " + command + " without proper Access Level.");
                    return;
                }

                if (Config.GMAUDIT) {
                    GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", _command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"));
                }

                ach.useAdminCommand(_command, activeChar);
            }
            else if (_command.startsWith("player_help ")) {
                playerHelp(activeChar, _command.substring(12));
            }
            else if (_command.startsWith("npc_")) {
                if (!activeChar.validateBypass(_command)) { return; }

                int endOfId = _command.indexOf('_', 5);
                String id;
                if (endOfId > 0) { id = _command.substring(4, endOfId); }
                else { id = _command.substring(4); }

                try {
                    final L2Object object = L2World.getInstance().getObject(Integer.parseInt(id));

                    if (object != null && object instanceof L2Npc && endOfId > 0 && ((L2Npc) object).canInteract(activeChar)) {
                        ((L2Npc) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
                    }

                    activeChar.sendPacket(ActionFailed.STATIC_PACKET);
                } catch (NumberFormatException nfe) {
                }
            }
            // Navigate throught Manor windows
            else if (_command.startsWith("manor_menu_select?")) {
                L2Object object = activeChar.getTarget();
                if (object instanceof L2Npc) { ((L2Npc) object).onBypassFeedback(activeChar, _command); }
            }
            else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block")) {
                CommunityBoard.getInstance().handleCommands(getClient(), _command);
            }
            else if (_command.startsWith("Quest ")) {
                if (!activeChar.validateBypass(_command)) { return; }

                String[] str = _command.substring(6).trim().split(" ", 2);
                if (str.length == 1) { activeChar.processQuestEvent(str[0], ""); }
                else { activeChar.processQuestEvent(str[0], str[1]); }
            }
            else if (_command.startsWith("_match")) {
                String params = _command.substring(_command.indexOf("?") + 1);
                StringTokenizer st = new StringTokenizer(params, "&");
                int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
                int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
                int heroid = Hero.getInstance().getHeroByClass(heroclass);
                if (heroid > 0) { Hero.getInstance().showHeroFights(activeChar, heroclass, heroid, heropage); }
            }
            else if (_command.startsWith("_diary")) {
                String params = _command.substring(_command.indexOf("?") + 1);
                StringTokenizer st = new StringTokenizer(params, "&");
                int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
                int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
                int heroid = Hero.getInstance().getHeroByClass(heroclass);
                if (heroid > 0) { Hero.getInstance().showHeroDiary(activeChar, heroclass, heroid, heropage); }
            }
            else if (_command.startsWith("arenachange")) // change
            {
                final boolean isManager = activeChar.getCurrentFolkNPC() instanceof L2OlympiadManagerInstance;
                if (!isManager) {
                    // Without npc, command can be used only in observer mode on arena
                    if (!activeChar.inObserverMode() || activeChar.isInOlympiadMode() || activeChar.getOlympiadGameId() < 0) {
                        return;
                    }
                }

                if (OlympiadManager.getInstance().isRegisteredInComp(activeChar)) {
                    activeChar.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
                    return;
                }

                final int arenaId = Integer.parseInt(_command.substring(12).trim());
                activeChar.enterOlympiadObserverMode(arenaId);
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "Bad RequestBypassToServer: ", e);
        }
    }

    private static void playerHelp(L2PcInstance activeChar, String path) {
        if (path.indexOf("..") != -1) { return; }

        final StringTokenizer st = new StringTokenizer(path);
        final String[] cmd = st.nextToken().split("#");

        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/help/" + cmd[0]);
        if (cmd.length > 1) { html.setItemId(Integer.parseInt(cmd[1])); }
        html.disableValidation();
        activeChar.sendPacket(html);
    }
}