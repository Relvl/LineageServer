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

/**
 * A container used for schemes buffer.
 */
public final class BuffSkillHolder extends IntIntHolder
{
	private final String _type;
	
	public BuffSkillHolder(int id, int price, String type)
	{
		super(id, price);
		
		_type = type;
	}
	
	public final String getType()
	{
		return _type;
	}
}