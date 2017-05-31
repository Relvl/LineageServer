/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as publishe d by the Free Software
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
package net.sf.l2j.gameserver.model.multisell;

public class Ingredient
{
	private int _itemId, _itemCount, _enchantmentLevel;
	private boolean _isTaxIngredient, _maintainIngredient;
	
	public Ingredient(int itemId, int itemCount, boolean isTaxIngredient, boolean maintainIngredient)
	{
		this(itemId, itemCount, 0, isTaxIngredient, maintainIngredient);
	}
	
	public Ingredient(int itemId, int itemCount, int enchantmentLevel, boolean isTaxIngredient, boolean mantainIngredient)
	{
		setItemId(itemId);
		setItemCount(itemCount);
		setEnchantmentLevel(enchantmentLevel);
		setIsTaxIngredient(isTaxIngredient);
		setMaintainIngredient(mantainIngredient);
	}
	
	public Ingredient(Ingredient e)
	{
		_itemId = e.getItemId();
		_itemCount = e.getItemCount();
		_enchantmentLevel = e.getEnchantmentLevel();
		_isTaxIngredient = e.isTaxIngredient();
		_maintainIngredient = e.getMaintainIngredient();
	}
	
	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public void setItemCount(int itemCount)
	{
		_itemCount = itemCount;
	}
	
	public int getItemCount()
	{
		return _itemCount;
	}
	
	public void setEnchantmentLevel(int enchantmentLevel)
	{
		_enchantmentLevel = enchantmentLevel;
	}
	
	public int getEnchantmentLevel()
	{
		return _enchantmentLevel;
	}
	
	public void setIsTaxIngredient(boolean isTaxIngredient)
	{
		_isTaxIngredient = isTaxIngredient;
	}
	
	public boolean isTaxIngredient()
	{
		return _isTaxIngredient;
	}
	
	public void setMaintainIngredient(boolean maintainIngredient)
	{
		_maintainIngredient = maintainIngredient;
	}
	
	public boolean getMaintainIngredient()
	{
		return _maintainIngredient;
	}
}