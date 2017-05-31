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

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class AdminRideWyvern implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ride",
		"admin_unride",
	};
	
	private int _petRideId;
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_ride"))
		{
			// command disabled if CW is worn. Warn user.
			if (activeChar.isCursedWeaponEquipped())
			{
				activeChar.sendMessage("You can't use //ride owning a Cursed Weapon.");
				return false;
			}
			
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (st.hasMoreTokens())
			{
				String mount = st.nextToken();
				
				if (mount.equals("wyvern") || mount.equals("2"))
					_petRideId = 12621;
				else if (mount.equals("strider") || mount.equals("1"))
					_petRideId = 12526;
				else
				{
					activeChar.sendMessage("Parameter '" + mount + "' isn't recognized for that command.");
					return false;
				}
			}
			else
			{
				activeChar.sendMessage("You must enter a parameter for that command.");
				return false;
			}
			
			// If code reached that place, it means _petRideId has been filled.
			if (activeChar.isMounted())
				activeChar.dismount();
			else if (activeChar.getPet() != null)
				activeChar.getPet().unSummon(activeChar);
			
			activeChar.mount(_petRideId, 0, false);
		}
		else if (command.equals("admin_unride"))
			activeChar.dismount();
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}