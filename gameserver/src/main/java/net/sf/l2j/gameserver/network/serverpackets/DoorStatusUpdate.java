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

import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class DoorStatusUpdate extends L2GameServerPacket
{
	private final L2DoorInstance _door;
	private final L2PcInstance _activeChar;
	
	public DoorStatusUpdate(L2DoorInstance door, L2PcInstance activeChar)
	{
		_door = door;
		_activeChar = activeChar;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x4d);
		writeD(_door.getObjectId());
		writeD(_door.isOpened() ? 0 : 1);
		writeD(_door.getDamage());
		writeD(_door.isAutoAttackable(_activeChar) ? 1 : 0);
		writeD(_door.getDoorId());
		writeD(_door.getMaxHp());
		writeD((int) _door.getCurrentHp());
	}
}