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

public final class L2EnchantSkillData
{
	private final int _costExp;
	private final int _costSp;
	private final int _itemId;
	private final int _itemCount;
	private final int _rate76;
	private final int _rate77;
	private final int _rate78;
	
	public L2EnchantSkillData(int costExp, int costSp, int itemId, int itemCount, int rate76, int rate77, int rate78)
	{
		_costExp = costExp;
		_costSp = costSp;
		_itemId = itemId;
		_itemCount = itemCount;
		_rate76 = rate76;
		_rate77 = rate77;
		_rate78 = rate78;
	}
	
	/**
	 * @return Returns the costExp.
	 */
	public int getCostExp()
	{
		return _costExp;
	}
	
	/**
	 * @return Returns the costSp.
	 */
	public int getCostSp()
	{
		return _costSp;
	}
	
	/**
	 * @return Returns the itemId.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * @return Returns the itemAmount.
	 */
	public int getItemCount()
	{
		return _itemCount;
	}
	
	/**
	 * @return Returns the rate according to level.
	 * @param level : Level determines the rate.
	 */
	public int getRate(int level)
	{
		switch (level)
		{
			case 76:
				return _rate76;
			case 77:
				return _rate77;
			case 78:
			case 79:
			case 80:
				return _rate78;
			default:
				return 0;
		}
	}
}