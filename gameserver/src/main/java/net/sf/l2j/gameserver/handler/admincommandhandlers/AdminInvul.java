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

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminInvul implements IAdminCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInvul.class);
    private static final String[] ADMIN_COMMANDS = {"admin_invul", "admin_setinvul"};

    private static void handleInvul(L2PcInstance activeChar) {
        String text;
        if (activeChar.isInvul()) {
            activeChar.setIsInvul(false);
            text = activeChar.getName() + " is now mortal.";
            LOGGER.info("GM: Gm removed invul mode from character {}({})", activeChar.getName(), activeChar.getObjectId());
        }
        else {
            activeChar.setIsInvul(true);
            text = activeChar.getName() + " is now invulnerable.";
            LOGGER.info("GM: Gm activated invul mode for character {}({})", activeChar.getName(), activeChar.getObjectId());
        }
        activeChar.sendMessage(text);
    }

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (command.equals("admin_invul")) { handleInvul(activeChar); }
        if (command.equals("admin_setinvul")) {
            L2Object target = activeChar.getTarget();
            if (target instanceof L2PcInstance) { handleInvul((L2PcInstance) target); }
        }
        return true;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}