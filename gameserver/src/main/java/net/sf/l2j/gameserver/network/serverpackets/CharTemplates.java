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

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.template.PcTemplate;

public class CharTemplates extends L2GameServerPacket
{
	private final List<PcTemplate> _chars = new ArrayList<>();
	
	public void addChar(PcTemplate template)
	{
		_chars.add(template);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x17);
		writeD(_chars.size());
		
		for (PcTemplate temp : _chars)
		{
			if (temp == null)
				continue;
			
			writeD(temp.getRace().ordinal());
			writeD(temp.getClassId().getId());
			writeD(0x46);
			writeD(temp.getBaseSTR());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseDEX());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseCON());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseINT());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseWIT());
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.getBaseMEN());
			writeD(0x0a);
		}
	}
}