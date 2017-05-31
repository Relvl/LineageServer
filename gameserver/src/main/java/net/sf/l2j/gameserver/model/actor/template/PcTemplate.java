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
package net.sf.l2j.gameserver.model.actor.template;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * @author mkizub
 */
public class PcTemplate extends CharTemplate
{
	private final ClassId _classId;
	private final Race _race;
	private final String _className;
	
	private final int _fallingHeight;
	
	private final double _collisionRadiusFemale;
	private final double _collisionHeightFemale;
	
	private final int _spawnX;
	private final int _spawnY;
	private final int _spawnZ;
	
	private final int _classBaseLevel;
	
	private final double[] _hpTable;
	private final double[] _mpTable;
	private final double[] _cpTable;
	
	private final List<Item> _items = new ArrayList<>();
	
	public PcTemplate(StatsSet set)
	{
		super(set);
		
		_classId = ClassId.values()[set.getInteger("classId")];
		_race = Race.values()[set.getInteger("raceId")];
		_className = set.getString("className");
		
		_fallingHeight = set.getInteger("falling_height", 333);
		
		_collisionRadiusFemale = set.getDouble("radiusFemale");
		_collisionHeightFemale = set.getDouble("heightFemale");
		
		_spawnX = set.getInteger("spawnX");
		_spawnY = set.getInteger("spawnY");
		_spawnZ = set.getInteger("spawnZ");
		
		_classBaseLevel = set.getInteger("baseLvl");
		
		// Feed HPs array from a String split.
		final String[] hpTable = set.getString("hpTable").split(";");
		
		_hpTable = new double[hpTable.length];
		for (int i = 0; i < hpTable.length; i++)
			_hpTable[i] = Double.parseDouble(hpTable[i]);
		
		// Feed MPs array from a String split.
		final String[] mpTable = set.getString("mpTable").split(";");
		
		_mpTable = new double[mpTable.length];
		for (int i = 0; i < mpTable.length; i++)
			_mpTable[i] = Double.parseDouble(mpTable[i]);
		
		// Feed CPs array from a String split.
		final String[] cpTable = set.getString("cpTable").split(";");
		
		_cpTable = new double[cpTable.length];
		for (int i = 0; i < cpTable.length; i++)
			_cpTable[i] = Double.parseDouble(cpTable[i]);
	}
	
	/**
	 * Add starter equipement.
	 * @param itemId the item to add if template is found
	 */
	public void addItem(int itemId)
	{
		final Item item = ItemTable.getInstance().getTemplate(itemId);
		if (item != null)
			_items.add(item);
	}
	
	/**
	 * @return itemIds of all the starter equipment
	 */
	public List<Item> getItems()
	{
		return _items;
	}
	
	public ClassId getClassId()
	{
		return _classId;
	}
	
	public Race getRace()
	{
		return _race;
	}
	
	public String getClassName()
	{
		return _className;
	}
	
	public int getFallHeight()
	{
		return _fallingHeight;
	}
	
	/**
	 * @param sex : True - female, False - male.
	 * @return : height depends on sex.
	 */
	@Override
	public int getCollisionRadius(boolean sex)
	{
		return (int) ((sex) ? _collisionRadiusFemale : _collisionRadius);
	}
	
	/**
	 * @param sex : True - female, False - male.
	 * @return : height depends on sex.
	 */
	@Override
	public int getCollisionHeight(boolean sex)
	{
		return (int) ((sex) ? _collisionHeightFemale : _collisionHeight);
	}
	
	public int getSpawnX()
	{
		return _spawnX;
	}
	
	public int getSpawnY()
	{
		return _spawnY;
	}
	
	public int getSpawnZ()
	{
		return _spawnZ;
	}
	
	public int getClassBaseLevel()
	{
		return _classBaseLevel;
	}
	
	@Override
	public double getBaseHpMax(int level)
	{
		return _hpTable[level - 1];
	}
	
	@Override
	public double getBaseMpMax(int level)
	{
		return _mpTable[level - 1];
	}
	
	public double getBaseCpMax(int level)
	{
		return _cpTable[level - 1];
	}
}