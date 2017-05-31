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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author -Wooden-
 */
public final class PledgeShowMemberListUpdate extends L2GameServerPacket
{
	private final int _pledgeType;
	private final int _hasSponsor;
	private final String _name;
	private final int _level;
	private final int _classId;
	private final int _isOnline;
	private final int _race;
	private final int _sex;
	
	public PledgeShowMemberListUpdate(L2PcInstance player)
	{
		_pledgeType = player.getPledgeType();
		_hasSponsor = (player.getSponsor() != 0 || player.getApprentice() != 0) ? 1 : 0;
		_name = player.getName();
		_level = player.getLevel();
		_classId = player.getClassId().getId();
		_race = player.getRace().ordinal();
		_sex = player.getAppearance().getSex() ? 1 : 0;
		_isOnline = (player.isOnline()) ? player.getObjectId() : 0;
	}
	
	public PledgeShowMemberListUpdate(L2ClanMember player)
	{
		_name = player.getName();
		_level = player.getLevel();
		_classId = player.getClassId();
		_isOnline = (player.isOnline()) ? player.getObjectId() : 0;
		_pledgeType = player.getPledgeType();
		_hasSponsor = (player.getSponsor() != 0 || player.getApprentice() != 0) ? 1 : 0;
		
		if (_isOnline != 0)
		{
			_race = player.getPlayerInstance().getRace().ordinal();
			_sex = (player.getPlayerInstance().getAppearance().getSex()) ? 1 : 0;
		}
		else
		{
			_sex = 0;
			_race = 0;
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x54);
		writeS(_name);
		writeD(_level);
		writeD(_classId);
		writeD(_sex);
		writeD(_race);
		writeD(_isOnline);
		writeD(_pledgeType);
		writeD(_hasSponsor);
	}
}