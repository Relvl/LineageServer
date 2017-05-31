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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * This class handles teleport admin commands
 */
public class AdminTeleport implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_runmod",
		"admin_instant_move",
		"admin_tele",
		"admin_tele_areas",
		"admin_goto",
		"admin_teleportto", // deprecated
		"admin_recall",
		"admin_recall_party",
		"admin_recall_clan",
		"admin_move_to",
		"admin_sendhome"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		// runmod
		if (command.equals("admin_runmod") || command.equals("admin_instant_move"))
			activeChar.setTeleMode(1);
		if (command.equals("admin_runmod tele"))
			activeChar.setTeleMode(2);
		if (command.equals("admin_runmod norm"))
			activeChar.setTeleMode(0);
		
		// teleport via panels
		if (command.equals("admin_tele"))
			AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
		if (command.equals("admin_tele_areas"))
			AdminHelpPage.showHelpPage(activeChar, "tele/other.htm");
		
		// recalls / goto types
		if (command.startsWith("admin_goto") || command.startsWith("admin_teleportto"))
		{
			StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String plyr = st.nextToken();
				L2PcInstance player = L2World.getInstance().getPlayer(plyr);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}
				
				teleportToCharacter(activeChar, player);
			}
		}
		else if (command.startsWith("admin_recall "))
		{
			try
			{
				String targetName = command.substring(13);
				L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}
				
				teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ());
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
		else if (command.startsWith("admin_recall_party"))
		{
			try
			{
				String targetName = command.substring(19);
				L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}
				
				if (player.isInParty())
				{
					for (L2PcInstance pm : player.getParty().getPartyMembers())
						teleportCharacter(pm, activeChar.getX(), activeChar.getY(), activeChar.getZ());
					
					activeChar.sendMessage("You recall " + player.getName() + "'s party.");
				}
				else
				{
					activeChar.sendMessage("You recall " + player.getName() + ", but he isn't in a party.");
					teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ());
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
		else if (command.startsWith("admin_recall_clan"))
		{
			try
			{
				String targetName = command.substring(18);
				L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}
				
				L2Clan clan = player.getClan();
				if (clan != null)
				{
					for (L2PcInstance member : clan.getOnlineMembers())
						teleportCharacter(member, activeChar.getX(), activeChar.getY(), activeChar.getZ());
					
					activeChar.sendMessage("You recall " + player.getName() + "'s clan.");
				}
				else
				{
					activeChar.sendMessage("You recall " + player.getName() + ", but he isn't a clan member.");
					teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ());
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
		else if (command.startsWith("admin_move_to"))
		{
			try
			{
				String val = command.substring(14);
				teleportTo(activeChar, val);
			}
			catch (Exception e)
			{
				// Case of empty or missing coordinates
				AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
			}
		}
		else if (command.startsWith("admin_sendhome"))
		{
			StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String plyr = st.nextToken();
				L2PcInstance player = L2World.getInstance().getPlayer(plyr);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}
				
				sendHome(player);
			}
			else
			{
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				
				// if target isn't a player, select yourself as target
				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					player = activeChar;
				
				sendHome(player);
			}
		}
		return true;
	}
	
	private static void sendHome(L2PcInstance player)
	{
		player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
		player.setIsIn7sDungeon(false);
		player.sendMessage("A GM sent you at nearest town.");
	}
	
	private static void teleportTo(L2PcInstance activeChar, String Cords)
	{
		try
		{
			StringTokenizer st = new StringTokenizer(Cords);
			String x1 = st.nextToken();
			int x = Integer.parseInt(x1);
			String y1 = st.nextToken();
			int y = Integer.parseInt(y1);
			String z1 = st.nextToken();
			int z = Integer.parseInt(z1);
			
			activeChar.getAI().setIntention(CtrlIntention.IDLE);
			activeChar.teleToLocation(x, y, z, 0);
			
			activeChar.sendMessage("You have been teleported to " + Cords + ".");
		}
		catch (NoSuchElementException nsee)
		{
			activeChar.sendMessage("Coordinates you entered as parameter [" + Cords + "] are wrong.");
		}
	}
	
	private static void teleportCharacter(L2PcInstance player, int x, int y, int z)
	{
		player.getAI().setIntention(CtrlIntention.IDLE);
		player.teleToLocation(x, y, z, 0);
		player.sendMessage("A GM is teleporting you.");
	}
	
	private static void teleportToCharacter(L2PcInstance activeChar, L2PcInstance target)
	{
		if (target.getObjectId() == activeChar.getObjectId())
			activeChar.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		else
		{
			int x = target.getX();
			int y = target.getY();
			int z = target.getZ();
			
			activeChar.getAI().setIntention(CtrlIntention.IDLE);
			activeChar.teleToLocation(x, y, z, 0);
			activeChar.sendMessage("You have teleported to " + target.getName() + ".");
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}