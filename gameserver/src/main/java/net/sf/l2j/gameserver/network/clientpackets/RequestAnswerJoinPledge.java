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

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.JoinPledge;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAdd;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestAnswerJoinPledge extends L2GameClientPacket
{
	private int _answer;
	
	@Override
	protected void readImpl()
	{
		_answer = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		L2PcInstance requestor = activeChar.getRequest().getPartner();
		if (requestor == null)
			return;
		
		if (_answer == 0)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_NOT_RESPOND_TO_S1_CLAN_INVITATION).addPcName(requestor));
			requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DID_NOT_RESPOND_TO_CLAN_INVITATION).addPcName(activeChar));
		}
		else
		{
			if (!(requestor.getRequest().getRequestPacket() instanceof RequestJoinPledge))
				return; // hax
				
			RequestJoinPledge requestPacket = (RequestJoinPledge) requestor.getRequest().getRequestPacket();
			L2Clan clan = requestor.getClan();
			
			// we must double check this cause during response time conditions can be changed, i.e. another player could join clan
			if (clan.checkClanJoinCondition(requestor, activeChar, requestPacket.getPledgeType()))
			{
				activeChar.sendPacket(new JoinPledge(requestor.getClanId()));
				
				activeChar.setPledgeType(requestPacket.getPledgeType());
				
				switch (requestPacket.getPledgeType())
				{
					case L2Clan.SUBUNIT_ACADEMY:
						activeChar.setPowerGrade(9);
						activeChar.setLvlJoinedAcademy(activeChar.getLevel());
						break;
					
					case L2Clan.SUBUNIT_ROYAL1:
					case L2Clan.SUBUNIT_ROYAL2:
						activeChar.setPowerGrade(7);
						break;
					
					case L2Clan.SUBUNIT_KNIGHT1:
					case L2Clan.SUBUNIT_KNIGHT2:
					case L2Clan.SUBUNIT_KNIGHT3:
					case L2Clan.SUBUNIT_KNIGHT4:
						activeChar.setPowerGrade(8);
						break;
					
					default:
						activeChar.setPowerGrade(6);
				}
				
				clan.addClanMember(activeChar);
				activeChar.setClanPrivileges(activeChar.getClan().getRankPrivs(activeChar.getPowerGrade()));
				
				activeChar.sendPacket(SystemMessageId.ENTERED_THE_CLAN);
				
				clan.broadcastToOtherOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_JOINED_CLAN).addPcName(activeChar), activeChar);
				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(activeChar), activeChar);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				
				// this activates the clan tab on the new member
				activeChar.sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), 0));
				for (SubPledge sp : activeChar.getClan().getAllSubPledges())
					activeChar.sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), sp.getId()));
				
				activeChar.setClanJoinExpiryTime(0);
				activeChar.broadcastUserInfo();
			}
		}
		activeChar.getRequest().onRequestResponse();
	}
}