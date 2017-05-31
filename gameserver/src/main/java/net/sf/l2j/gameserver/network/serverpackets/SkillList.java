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

public final class SkillList extends L2GameServerPacket
{
	private final List<Skill> _skills;
	
	static class Skill
	{
		public int id;
		public int level;
		public boolean passive;
		public boolean disabled;
		
		Skill(int pId, int pLevel, boolean pPassive, boolean pDisabled)
		{
			id = pId;
			level = pLevel;
			passive = pPassive;
			disabled = pDisabled;
		}
	}
	
	public SkillList()
	{
		_skills = new ArrayList<>();
	}
	
	public void addSkill(int id, int level, boolean passive, boolean disabled)
	{
		_skills.add(new Skill(id, level, passive, disabled));
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x58);
		writeD(_skills.size());
		
		for (Skill temp : _skills)
		{
			writeD(temp.passive ? 1 : 0);
			writeD(temp.level);
			writeD(temp.id);
			writeC(temp.disabled ? 1 : 0);
		}
	}
}