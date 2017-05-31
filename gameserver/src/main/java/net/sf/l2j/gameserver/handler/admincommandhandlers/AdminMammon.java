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
import net.sf.l2j.gameserver.instancemanager.AutoSpawnManager;
import net.sf.l2j.gameserver.instancemanager.AutoSpawnManager.AutoSpawnInstance;
import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Admin Command Handler for Mammon NPCs
 * @author Tempy
 */
public class AdminMammon implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_mammon_find",
		"admin_mammon_respawn",
		"admin_msg"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_mammon_find"))
		{
			int teleportIndex = -1;
			
			try
			{
				teleportIndex = Integer.parseInt(command.substring(18));
			}
			catch (Exception NumberFormatException)
			{
				activeChar.sendMessage("Usage: //mammon_find [teleportIndex] (1 / 2)");
				return false;
			}
			
			if (!SevenSigns.getInstance().isSealValidationPeriod())
			{
				activeChar.sendMessage("The competition period is currently in effect.");
				return true;
			}
			
			if (teleportIndex == 1)
			{
				final AutoSpawnInstance blackSpawnInst = AutoSpawnManager.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_BLACKSMITH_ID, false);
				if (blackSpawnInst != null)
				{
					final L2Npc[] blackInst = blackSpawnInst.getNPCInstanceList();
					if (blackInst.length > 0)
					{
						final int x1 = blackInst[0].getX(), y1 = blackInst[0].getY(), z1 = blackInst[0].getZ();
						activeChar.sendMessage("Blacksmith of Mammon: " + x1 + " " + y1 + " " + z1);
						activeChar.teleToLocation(x1, y1, z1, 0);
					}
				}
				else
					activeChar.sendMessage("Blacksmith of Mammon isn't registered.");
			}
			else if (teleportIndex == 2)
			{
				final AutoSpawnInstance merchSpawnInst = AutoSpawnManager.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_MERCHANT_ID, false);
				if (merchSpawnInst != null)
				{
					final L2Npc[] merchInst = merchSpawnInst.getNPCInstanceList();
					if (merchInst.length > 0)
					{
						final int x2 = merchInst[0].getX(), y2 = merchInst[0].getY(), z2 = merchInst[0].getZ();
						activeChar.sendMessage("Merchant of Mammon: " + x2 + " " + y2 + " " + z2);
						activeChar.teleToLocation(x2, y2, z2, 0);
					}
				}
				else
					activeChar.sendMessage("Merchant of Mammon isn't registered.");
			}
			else
				activeChar.sendMessage("Invalid parameter '" + teleportIndex + "' for //mammon_find.");
		}
		else if (command.startsWith("admin_mammon_respawn"))
		{
			if (!SevenSigns.getInstance().isSealValidationPeriod())
			{
				activeChar.sendMessage("The competition period is currently in effect.");
				return true;
			}
			
			final AutoSpawnInstance merchSpawnInst = AutoSpawnManager.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_MERCHANT_ID, false);
			if (merchSpawnInst != null)
			{
				long merchRespawn = AutoSpawnManager.getInstance().getTimeToNextSpawn(merchSpawnInst);
				activeChar.sendMessage("The Merchant of Mammon will respawn in " + (merchRespawn / 60000) + " minute(s).");
			}
			else
				activeChar.sendMessage("Merchant of Mammon isn't registered.");
			
			final AutoSpawnInstance blackSpawnInst = AutoSpawnManager.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_BLACKSMITH_ID, false);
			if (blackSpawnInst != null)
			{
				long blackRespawn = AutoSpawnManager.getInstance().getTimeToNextSpawn(blackSpawnInst);
				activeChar.sendMessage("The Blacksmith of Mammon will respawn in " + (blackRespawn / 60000) + " minute(s).");
			}
			else
				activeChar.sendMessage("Blacksmith of Mammon isn't registered.");
		}
		// Used for testing SystemMessage IDs - Use //msg <ID>
		else if (command.startsWith("admin_msg"))
		{
			try
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(Integer.parseInt(command.substring(10).trim())));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Command format: //msg <SYSTEM_MSG_ID>");
				return false;
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