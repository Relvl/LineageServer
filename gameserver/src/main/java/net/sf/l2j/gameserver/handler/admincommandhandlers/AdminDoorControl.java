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

import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * This class handles following admin commands
 * <ul>
 * <li>open = open a door using a doorId, or a targeted door if not found.</li>
 * <li>close = close a door using a doorId, or a targeted door if not found.</li>
 * <li>openall = open all doors registered on doors.xml.</li>
 * <li>closeall = close all doors registered on doors.xml.</li>
 * </ul>
 */
public class AdminDoorControl implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_open",
		"admin_close",
		"admin_openall",
		"admin_closeall"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_open"))
		{
			if (command.equals("admin_openall"))
			{
				for (L2DoorInstance door : DoorTable.getInstance().getDoors())
					door.openMe();
			}
			else
			{
				try
				{
					final L2DoorInstance door = DoorTable.getInstance().getDoor(Integer.parseInt(command.substring(11)));
					if (door != null)
						door.openMe();
					else
						activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				catch (Exception e)
				{
					final L2Object target = activeChar.getTarget();
					
					if (target instanceof L2DoorInstance)
						((L2DoorInstance) target).openMe();
					else
						activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
		}
		else if (command.startsWith("admin_close"))
		{
			if (command.equals("admin_closeall"))
			{
				for (L2DoorInstance door : DoorTable.getInstance().getDoors())
					door.closeMe();
			}
			else
			{
				try
				{
					final L2DoorInstance door = DoorTable.getInstance().getDoor(Integer.parseInt(command.substring(12)));
					if (door != null)
						door.closeMe();
					else
						activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				catch (Exception e)
				{
					final L2Object target = activeChar.getTarget();
					
					if (target instanceof L2DoorInstance)
						((L2DoorInstance) target).closeMe();
					else
						activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}