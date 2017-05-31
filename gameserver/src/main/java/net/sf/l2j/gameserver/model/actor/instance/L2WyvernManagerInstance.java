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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This instance leads the behavior of Wyvern Managers.<br>
 * Those NPCs allow Castle Lords to mount a wyvern in return for B Crystals.<br>
 * Three configs exist so far :<br>
 * <ul>
 * <li>WYVERN_ALLOW_UPGRADER : spawn instances of Wyvern Manager through the world, or no;</li>
 * <li>WYVERN_REQUIRED_LEVEL : the strider's required level;</li>
 * <li>WYVERN_REQUIRED_CRYSTALS : the B-crystals' required amount;</li>
 * </ul>
 * @author Tryskell
 */
public class L2WyvernManagerInstance extends L2CastleChamberlainInstance
{
	final private int neededCrystals = Config.WYVERN_REQUIRED_CRYSTALS;
	final private int requiredLevel = Config.WYVERN_REQUIRED_LEVEL;
	
	public L2WyvernManagerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (player.getCurrentFolkNPC() == null || player.getCurrentFolkNPC().getObjectId() != getObjectId())
			return;
		
		if (command.startsWith("RideWyvern"))
		{
			String val = "2";
			if (player.isClanLeader())
			{
				// Verify if Dusk own Seal of Strife (if true, CLs can't mount wyvern).
				if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
					val = "3";
				// If player is mounted on a strider
				else if (player.isMounted() && (player.getMountNpcId() == 12526 || player.getMountNpcId() == 12527 || player.getMountNpcId() == 12528))
				{
					// Check for strider level
					if (player.getMountLevel() < requiredLevel)
						val = "6";
					// Check for items consumption
					else if (player.destroyItemByItemId("Wyvern", 1460, neededCrystals, player, true))
					{
						player.dismount();
						if (player.mount(12621, 0, true))
							val = "4";
					}
					else
						val = "5";
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_MAY_ONLY_RIDE_WYVERN_WHILE_RIDING_STRIDER);
					val = "1";
				}
			}
			
			sendHtm(player, val);
		}
		else if (command.startsWith("Chat"))
		{
			String val = "1"; // Default send you to error HTM.
			try
			{
				val = command.substring(5);
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			
			sendHtm(player, val);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String val = "0a"; // Default value : player's clan doesn't own castle.
		
		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_OWNER) // Clan owns castle && player is CL ; send the good HTM.
			{
				if (player.isFlying()) // Already mounted on Wyvern
					val = "4";
				else
					val = "0"; // Initial screen
			}
			else if (condition == COND_CLAN_MEMBER) // Good clan, but player isn't a CL.
				val = "2";
		}
		sendHtm(player, val);
	}
	
	private void sendHtm(L2PcInstance player, String val)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/wyvernmanager/wyvernmanager-" + val + ".htm");
		html.replace("%objectId%", getObjectId());
		html.replace("%npcname%", getName());
		html.replace("%wyvern_level%", requiredLevel);
		html.replace("%needed_crystals%", neededCrystals);
		player.sendPacket(html);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}