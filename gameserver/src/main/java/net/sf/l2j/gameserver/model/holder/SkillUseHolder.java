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
package net.sf.l2j.gameserver.model.holder;

import net.sf.l2j.gameserver.model.L2Skill;

/**
 * Skill casting information (used to queue when several skills are cast in a short time)
 **/
public class SkillUseHolder
{
	private L2Skill _skill;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	public SkillUseHolder()
	{
	}
	
	public SkillUseHolder(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
	{
		_skill = skill;
		_ctrlPressed = ctrlPressed;
		_shiftPressed = shiftPressed;
	}
	
	public L2Skill getSkill()
	{
		return _skill;
	}
	
	public int getSkillId()
	{
		return (getSkill() != null) ? getSkill().getId() : -1;
	}
	
	public boolean isCtrlPressed()
	{
		return _ctrlPressed;
	}
	
	public boolean isShiftPressed()
	{
		return _shiftPressed;
	}
	
	public void setSkill(L2Skill skill)
	{
		_skill = skill;
	}
	
	public void setCtrlPressed(boolean ctrlPressed)
	{
		_ctrlPressed = ctrlPressed;
	}
	
	public void setShiftPressed(boolean shiftPressed)
	{
		_shiftPressed = shiftPressed;
	}
}