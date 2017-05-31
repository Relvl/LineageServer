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
package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Map;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Support for clan penalty user command.
 * @author Tempy
 */
public class ClanPenalty implements IUserCommandHandler
{
	private static final String NO_PENALTY = "<tr><td width=170>No penalty is imposed.</td><td width=100 align=center></td></tr>";
	
	private static final int[] COMMAND_IDS =
	{
		100
	};
	
	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		final StringBuilder sb = new StringBuilder();
		
		// Join a clan penalty.
		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
			StringUtil.append(sb, "<tr><td width=170>Unable to join a clan.</td><td width=100 align=center>", sdf.format(activeChar.getClanJoinExpiryTime()), "</td></tr>");
		
		// Create a clan penalty.
		if (activeChar.getClanCreateExpiryTime() > System.currentTimeMillis())
			StringUtil.append(sb, "<tr><td width=170>Unable to create a clan.</td><td width=100 align=center>", sdf.format(activeChar.getClanCreateExpiryTime()), "</td></tr>");
		
		final L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			// Invitation in a clan penalty.
			if (clan.getCharPenaltyExpiryTime() > System.currentTimeMillis())
				StringUtil.append(sb, "<tr><td width=170>Unable to invite a clan member.</td><td width=100 align=center>", sdf.format(clan.getCharPenaltyExpiryTime()), "</td></tr>");
			
			// War penalty.
			if (!clan.getWarPenalty().isEmpty())
			{
				for (Map.Entry<Integer, Long> entry : clan.getWarPenalty().entrySet())
				{
					if (entry.getValue() > System.currentTimeMillis())
					{
						final L2Clan enemyClan = ClanTable.getInstance().getClan(entry.getKey());
						if (enemyClan != null)
							StringUtil.append(sb, "<tr><td width=170>Unable to attack ", enemyClan.getName(), " clan.</td><td width=100 align=center>", sdf.format(entry.getValue()), "</td></tr>");
					}
				}
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/clan_penalty.htm");
		html.replace("%content%", (sb.length() == 0) ? NO_PENALTY : sb.toString());
		activeChar.sendPacket(html);
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}