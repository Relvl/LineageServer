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
package net.sf.l2j.gameserver.model.item.type;

/**
 * @author mkizub
 */
public enum WeaponType implements ItemType
{
	NONE(40),
	SWORD(40),
	BLUNT(40),
	DAGGER(40),
	BOW(500),
	POLE(66),
	ETC(40),
	FIST(40),
	DUAL(40),
	DUALFIST(40),
	BIGSWORD(40),
	FISHINGROD(40),
	BIGBLUNT(40),
	PET(40);
	
	private final int _mask;
	private final int _range;
	
	private WeaponType(int range)
	{
		_mask = 1 << ordinal();
		_range = range;
	}
	
	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	@Override
	public int mask()
	{
		return _mask;
	}
	
	public int getRange()
	{
		return _range;
	}
}