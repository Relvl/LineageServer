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

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.zone.type.L2SiegeZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class SiegeStatus implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		99
	};
	
	private static final String IN_PROGRESS = "Castle Siege in Progress";
	private static final String OUTSIDE_ZONE = "Outside Castle Siege Zone";
	
	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (!activeChar.isClanLeader())
		{
			activeChar.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CAN_ISSUE_COMMANDS);
			return false;
		}
		
		if (!activeChar.isNoble())
		{
			activeChar.sendPacket(SystemMessageId.ONLY_NOBLESSE_LEADER_CAN_VIEW_SIEGE_STATUS_WINDOW);
			return false;
		}
		
		final L2Clan clan = activeChar.getClan();
		
		final StringBuilder sb = new StringBuilder();
		
		for (Siege siege : SiegeManager.getSieges())
		{
			if (!siege.isInProgress())
				continue;
			
			// Search on lists : as a clan can only be registered in a single siege, break after one case is found.
			if (siege.getAttackerClan(clan.getClanId()) != null || siege.getDefenderClan(clan.getClanId()) != null)
			{
				final L2SiegeZone zone = siege.getCastle().getZone();
				for (L2PcInstance member : clan.getOnlineMembers())
					StringUtil.append(sb, "<tr><td width=170>", member.getName(), "</td><td width=100>", (zone.isInsideZone(member.getX(), member.getY(), member.getZ())) ? IN_PROGRESS : OUTSIDE_ZONE, "</td></tr>");
				
				final NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setFile("data/html/siege_status.htm");
				html.replace("%kills%", clan.getSiegeKills());
				html.replace("%deaths%", clan.getSiegeDeaths());
				html.replace("%content%", sb.toString());
				activeChar.sendPacket(html);
				return true;
			}
		}
		
		activeChar.sendPacket(SystemMessageId.ONLY_DURING_SIEGE);
		return false;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}