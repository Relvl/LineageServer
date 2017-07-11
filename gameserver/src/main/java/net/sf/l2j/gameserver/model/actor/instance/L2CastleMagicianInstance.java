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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

/**
 * @author Kerberos | ZaKaX
 */
public class L2CastleMagicianInstance extends L2NpcInstance
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	
	public L2CastleMagicianInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/castlemagician/magician-no.htm";
		
		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castlemagician/magician-busy.htm"; // Busy because of siege
			else if (condition == COND_OWNER) // Clan owns castle
			{
				if (val == 0)
					filename = "data/html/castlemagician/magician.htm";
				else
					filename = "data/html/castlemagician/magician-" + val + ".htm";
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			showChatWindow(player, val);
			return;
		}
		else if (command.equals("gotoleader"))
		{
			if (player.getClan() != null)
			{
				L2PcInstance clanLeader = player.getClan().getLeader().getPlayerInstance();
				if (clanLeader == null)
					return;
				
				if (clanLeader.getFirstEffect(L2EffectType.CLAN_GATE) != null)
				{
					if (!validateGateCondition(clanLeader, player))
						return;
					
					player.teleToLocation(clanLeader.getX(), clanLeader.getY(), clanLeader.getZ(), 0);
					return;
				}
				String filename = "data/html/castlemagician/magician-nogate.htm";
				showChatWindow(player, filename);
			}
			return;
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	protected int validateCondition(L2PcInstance player)
	{
		if (getCastle() != null && player.getClan() != null)
		{
			if (getCastle().getZone().isActive())
				return COND_BUSY_BECAUSE_OF_SIEGE;
			
			if (getCastle().getOwnerId() == player.getClanId())
				return COND_OWNER;
		}
		return COND_ALL_FALSE;
	}
	
	private static final boolean validateGateCondition(L2PcInstance clanLeader, L2PcInstance player)
	{
		if (clanLeader.isAlikeDead() || clanLeader.isInStoreMode() || clanLeader.isRooted() || clanLeader.isInCombat() || clanLeader.isInOlympiadMode() || clanLeader.isFestivalParticipant() || clanLeader.inObserverMode() || clanLeader.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (player.isIn7sDungeon())
		{
			final int targetCabal = SevenSigns.getInstance().getPlayerCabal(clanLeader.getObjectId());
			if (SevenSigns.getInstance().isSealValidationPeriod())
			{
				if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
				{
					player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
					return false;
				}
			}
			else
			{
				if (targetCabal == SevenSigns.CABAL_NULL)
				{
					player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
					return false;
				}
			}
		}
		
		return true;
	}
}