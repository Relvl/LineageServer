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
package net.sf.l2j.gameserver.model;

public final class L2SkillLearn
{
	private final int _id;
	private final int _level;
	private final int _spCost;
	private final int _minLevel;
	private final int _costid;
	private final int _costcount;
	
	public L2SkillLearn(int id, int lvl, int minLvl, int cost, int costid, int costcount)
	{
		_id = id;
		_level = lvl;
		_minLevel = minLvl;
		_spCost = cost;
		_costid = costid;
		_costcount = costcount;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public int getSpCost()
	{
		return _spCost;
	}
	
	public int getIdCost()
	{
		return _costid;
	}
	
	public int getCostCount()
	{
		return _costcount;
	}
}