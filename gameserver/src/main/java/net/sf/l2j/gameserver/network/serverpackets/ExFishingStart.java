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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;

/**
 * Format (ch)ddddd
 * @author -Wooden-
 */
public class ExFishingStart extends L2GameServerPacket
{
	private final L2Character _activeChar;
	private final Location _loc;
	private final int _fishType;
	private final boolean _isNightLure;
	
	public ExFishingStart(L2Character character, int fishType, Location loc, boolean isNightLure)
	{
		_activeChar = character;
		_fishType = fishType;
		_loc = loc;
		_isNightLure = isNightLure;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x13);
		writeD(_activeChar.getObjectId());
		writeD(_fishType); // fish type
		writeD(_loc.getX()); // x position
		writeD(_loc.getY()); // y position
		writeD(_loc.getZ()); // z position
		writeC(_isNightLure ? 0x01 : 0x00); // night lure
		writeC(Config.ALT_FISH_CHAMPIONSHIP_ENABLED ? 0x01 : 0x00); // show fish rank result button
	}
}