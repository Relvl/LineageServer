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
package net.sf.l2j.gameserver.model.soulcrystal;

public final class SoulCrystalData
{
	private final int _level;
	private final int _crystalItemId;
	private final int _stagedItemId;
	private final int _brokenItemId;
	
	public SoulCrystalData(int level, int crystalItemId, int stagedItemId, int brokenItemId)
	{
		_level = level;
		_crystalItemId = crystalItemId;
		_stagedItemId = stagedItemId;
		_brokenItemId = brokenItemId;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getCrystalItemId()
	{
		return _crystalItemId;
	}
	
	public int getStagedItemId()
	{
		return _stagedItemId;
	}
	
	public int getBrokenItemId()
	{
		return _brokenItemId;
	}
}