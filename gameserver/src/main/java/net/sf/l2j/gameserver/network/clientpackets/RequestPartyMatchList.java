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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyRoomMember;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchDetail;

public class RequestPartyMatchList extends L2GameClientPacket
{
	private int _roomid;
	private int _membersmax;
	private int _lvlmin;
	private int _lvlmax;
	private int _loot;
	private String _roomtitle;
	
	@Override
	protected void readImpl()
	{
		_roomid = readD();
		_membersmax = readD();
		_lvlmin = readD();
		_lvlmax = readD();
		_loot = readD();
		_roomtitle = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		if (_roomid > 0)
		{
			PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_roomid);
			if (room != null)
			{
				_log.info("PartyMatchRoom #" + room.getId() + " changed by " + activeChar.getName());
				room.setMaxMembers(_membersmax);
				room.setMinLvl(_lvlmin);
				room.setMaxLvl(_lvlmax);
				room.setLootType(_loot);
				room.setTitle(_roomtitle);
				
				for (L2PcInstance member : room.getPartyMembers())
				{
					if (member == null)
						continue;
					
					member.sendPacket(new PartyMatchDetail(room));
					member.sendPacket(SystemMessageId.PARTY_ROOM_REVISED);
				}
			}
		}
		else
		{
			int maxid = PartyMatchRoomList.getInstance().getMaxId();
			
			PartyMatchRoom room = new PartyMatchRoom(maxid, _roomtitle, _loot, _lvlmin, _lvlmax, _membersmax, activeChar);
			
			_log.info("PartyMatchRoom #" + maxid + " created by " + activeChar.getName());
			
			// Remove from waiting list, and add to current room
			PartyMatchWaitingList.getInstance().removePlayer(activeChar);
			PartyMatchRoomList.getInstance().addPartyMatchRoom(maxid, room);
			
			if (activeChar.isInParty())
			{
				for (L2PcInstance ptmember : activeChar.getParty().getPartyMembers())
				{
					if (ptmember == null || ptmember == activeChar)
						continue;
					
					ptmember.setPartyRoom(maxid);
					
					room.addMember(ptmember);
				}
			}
			
			activeChar.sendPacket(new PartyMatchDetail(room));
			activeChar.sendPacket(new ExPartyRoomMember(room, 1));
			
			activeChar.sendPacket(SystemMessageId.PARTY_ROOM_CREATED);
			
			activeChar.setPartyRoom(maxid);
			activeChar.broadcastUserInfo();
		}
	}
}