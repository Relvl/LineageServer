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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.Shutdown;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class AdminMaintenance implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
            {
                    "admin_server",

                    "admin_server_shutdown",
                    "admin_server_restart",
                    "admin_server_abort",
            };

    private static void sendHtmlForm(L2PcInstance activeChar) {
        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/admin/maintenance.htm");
        html.replace("%count%", L2World.getInstance().getPlayers().size());
        html.replace("%used%", Math.round((int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576)));
        html.replace("%status%", LoginServerThread.getInstance().getStatusString());
        html.replace("%time%", GameTimeTaskManager.getInstance().getGameTimeFormated());
        activeChar.sendPacket(html);
    }

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (command.equals("admin_server")) {
            sendHtmlForm(activeChar);
        }
        else if (command.startsWith("admin_server_shutdown")) {
            try {
                Shutdown.getInstance().startShutdown(activeChar, null, Integer.parseInt(command.substring(22)), false);
            } catch (StringIndexOutOfBoundsException e) {
                sendHtmlForm(activeChar);
            }
        }
        else if (command.startsWith("admin_server_restart")) {
            try {
                Shutdown.getInstance().startShutdown(activeChar, null, Integer.parseInt(command.substring(21)), true);
            } catch (StringIndexOutOfBoundsException e) {
                sendHtmlForm(activeChar);
            }
        }
        else if (command.startsWith("admin_server_abort")) {
            Shutdown.getInstance().abort(activeChar);
        }
        return true;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}