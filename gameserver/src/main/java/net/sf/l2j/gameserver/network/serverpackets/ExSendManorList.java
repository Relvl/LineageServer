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

/**
 * Format : (h) d [dS]
 * @author l3x
 */
public class ExSendManorList extends L2GameServerPacket
{
	public static final ExSendManorList STATIC_PACKET = new ExSendManorList();
	
	private ExSendManorList()
	{
	}
	
	private static final String[] _manorList =
	{
		"gludio",
		"dion",
		"giran",
		"oren",
		"aden",
		"innadril",
		"goddard",
		"rune",
		"schuttgart"
	};
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x1B);
		writeD(_manorList.length);
		for (int i = 0; i < _manorList.length; i++)
		{
			writeD(i + 1);
			writeS(_manorList[i]);
		}
	}
}