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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author godson
 */
public class ExOlympiadUserInfo extends L2GameServerPacket
{
	private int _side;
	private int _objectId;
	private String _name;
	private int _classId;
	private int _curHp;
	private int _maxHp;
	private int _curCp;
	private int _maxCp;
	
	public ExOlympiadUserInfo(L2PcInstance player)
	{
		_side = player.getOlympiadSide();
		_objectId = player.getObjectId();
		_name = player.getName();
		_classId = player.getClassId().getId();
		_curHp = (int) player.getCurrentHp();
		_maxHp = player.getMaxHp();
		_curCp = (int) player.getCurrentCp();
		_maxCp = player.getMaxCp();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x29);
		writeC(_side);
		writeD(_objectId);
		writeS(_name);
		writeD(_classId);
		writeD(_curHp);
		writeD(_maxHp);
		writeD(_curCp);
		writeD(_maxCp);
	}
}