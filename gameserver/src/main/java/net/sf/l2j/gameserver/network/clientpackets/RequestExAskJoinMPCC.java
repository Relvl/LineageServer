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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExAskJoinMPCC;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) S
 * @author chris_00
 */
public final class RequestExAskJoinMPCC extends L2GameClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		L2PcInstance player = L2World.getInstance().getPlayer(_name);
		if (player == null)
			return;
		
		// You can't invite yourself
		if (activeChar.isInParty() && player.isInParty() && activeChar.getParty().equals(player.getParty()))
			return;
		
		// activeChar is in a Party?
		if (activeChar.isInParty())
		{
			L2Party activeParty = activeChar.getParty();
			// activeChar is PartyLeader? && activeChars Party is already in a CommandChannel?
			if (activeParty.getLeader().equals(activeChar))
			{
				// if activeChars Party is in CC, is activeChar CCLeader?
				if (activeParty.isInCommandChannel() && activeParty.getCommandChannel().getChannelLeader().equals(activeChar))
				{
					// target is in a party?
					if (player.isInParty())
					{
						// targets party already in a CChannel?
						if (player.getParty().isInCommandChannel())
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL).addPcName(player));
						// ready to open a new CC, send request to targets Party's PartyLeader
						else
							askJoinMPCC(activeChar, player);
					}
					else
						activeChar.sendMessage(player.getName() + " doesn't have party and cannot be invited to Command Channel.");
				}
				// in CC, but not the CCLeader
				else if (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getChannelLeader().equals(activeChar))
					activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
				else
				{
					// target in a party?
					if (player.isInParty())
					{
						// target's party already in a CChannel?
						if (player.getParty().isInCommandChannel())
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_MEMBER_OF_COMMAND_CHANNEL).addPcName(player));
						// ready to open a new CC, send request to targets Party's PartyLeader
						else
							askJoinMPCC(activeChar, player);
					}
					else
						activeChar.sendMessage(player.getName() + " doesn't have party and cannot be invited to Command Channel.");
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
		}
	}
	
	private static void askJoinMPCC(L2PcInstance requestor, L2PcInstance target)
	{
		boolean hasRight = false;
		
		if (requestor.getClan() != null && requestor.getClan().getLeaderId() == requestor.getObjectId() && requestor.getClan().getLevel() >= 5) // Clanleader of lvl5 Clan or higher
			hasRight = true;
		else if (requestor.getInventory().getItemByItemId(8871) != null) // 8871 Strategy Guide. Should destroyed after sucessfull invite?
			hasRight = true;
		else if (requestor.getPledgeClass() >= 5) // At least Baron or higher
		{
			for (L2Skill skill : requestor.getAllSkills())
			{
				// Skill Clan Imperium
				if (skill.getId() == 391)
				{
					hasRight = true;
					break;
				}
			}
		}
		
		if (!hasRight)
		{
			requestor.sendPacket(SystemMessageId.COMMAND_CHANNEL_ONLY_BY_LEVEL_5_CLAN_LEADER_PARTY_LEADER);
			return;
		}
		
		// Get the target's party leader, and do whole actions on him.
		L2PcInstance targetLeader = target.getParty().getLeader();
		if (!targetLeader.isProcessingRequest())
		{
			requestor.onTransactionRequest(targetLeader);
			targetLeader.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COMMAND_CHANNEL_CONFIRM_FROM_S1).addPcName(requestor));
			targetLeader.sendPacket(new ExAskJoinMPCC(requestor.getName()));
		}
		else
			requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addPcName(targetLeader));
	}
}