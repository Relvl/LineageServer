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

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.GMViewPledgeInfo;

/**
 * This handler handles pledge commands.<br>
 * <br>
 * With any player target:
 * <ul>
 * <li>//pledge create <b>String</b></li>
 * </ul>
 * With clan member target:
 * <ul>
 * <li>//pledge info</li>
 * <li>//pledge dismiss</li>
 * <li>//pledge setlevel <b>int</b></li>
 * <li>//pledge rep <b>int</b></li>
 * </ul>
 */
public class AdminPledge implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pledge"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		final L2Object target = activeChar.getTarget();
		if (!(target instanceof L2PcInstance))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			showMainPage(activeChar);
			return false;
		}
		
		if (command.startsWith("admin_pledge"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final String action = st.nextToken();
				final L2PcInstance player = (L2PcInstance) target;
				
				if (action.equals("create"))
				{
					try
					{
						final String parameter = st.nextToken();
						
						long cet = player.getClanCreateExpiryTime();
						player.setClanCreateExpiryTime(0);
						L2Clan clan = ClanTable.getInstance().createClan(player, parameter);
						if (clan != null)
							activeChar.sendMessage("Clan " + parameter + " have been created. Clan leader is " + player.getName() + ".");
						else
						{
							player.setClanCreateExpiryTime(cet);
							activeChar.sendMessage("There was a problem while creating the clan.");
						}
					}
					catch (Exception e)
					{
						activeChar.sendMessage("Invalid string parameter for //pledge create.");
					}
				}
				else
				{
					if (player.getClan() == null)
					{
						activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
						showMainPage(activeChar);
						return false;
					}
					
					if (action.equals("dismiss"))
					{
						ClanTable.getInstance().destroyClan(player.getClanId());
						L2Clan clan = player.getClan();
						if (clan == null)
							activeChar.sendMessage("The clan is now disbanded.");
						else
							activeChar.sendMessage("There was a problem while destroying the clan.");
					}
					else if (action.equals("info"))
						activeChar.sendPacket(new GMViewPledgeInfo(player.getClan(), player));
					else if (action.equals("setlevel"))
					{
						try
						{
							final int level = Integer.parseInt(st.nextToken());
							
							if (level >= 0 && level < 9)
							{
								player.getClan().changeLevel(level);
								activeChar.sendMessage("You have set clan " + player.getClan().getName() + " to level " + level);
							}
							else
								activeChar.sendMessage("This clan level is incorrect. Put a number between 0 and 8.");
						}
						catch (Exception e)
						{
							activeChar.sendMessage("Invalid number parameter for //pledge setlevel.");
						}
					}
					else if (action.startsWith("rep"))
					{
						try
						{
							final int points = Integer.parseInt(st.nextToken());
							final L2Clan clan = player.getClan();
							
							if (clan.getLevel() < 5)
							{
								activeChar.sendMessage("Only clans of level 5 or above may receive reputation points.");
								showMainPage(activeChar);
								return false;
							}
							
							clan.addReputationScore(points);
							activeChar.sendMessage("You " + (points > 0 ? "added " : "removed ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Their current score is: " + clan.getReputationScore());
						}
						catch (Exception e)
						{
							activeChar.sendMessage("Invalid number parameter for //pledge rep.");
						}
					}
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Invalid action or parameter.");
			}
		}
		showMainPage(activeChar);
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static void showMainPage(L2PcInstance activeChar)
	{
		AdminHelpPage.showHelpPage(activeChar, "game_menu.htm");
	}
}