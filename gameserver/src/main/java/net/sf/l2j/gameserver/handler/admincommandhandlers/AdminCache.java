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

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Layanere
 */
public class AdminCache implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_reload_cache_path",
		"admin_reload_cache_file",
	};
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_reload_cache_path "))
		{
			try
			{
				final String path = command.split(" ")[1];
				HtmCache.getInstance().reloadPath(path);
				activeChar.sendMessage("HTM paths' cache have been reloaded.");
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //reload_cache_path <path>");
			}
		}
		else if (command.startsWith("admin_reload_cache_file "))
		{
			try
			{
				String path = command.split(" ")[1];
				if (HtmCache.getInstance().isLoadable(path))
					activeChar.sendMessage("Cache[HTML]: requested file was loaded.");
				else
					activeChar.sendMessage("Cache[HTML]: requested file couldn't be loaded.");
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //reload_cache_file <relative_path/file>");
			}
		}
		return true;
	}
}