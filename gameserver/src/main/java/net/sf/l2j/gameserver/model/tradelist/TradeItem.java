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
package net.sf.l2j.gameserver.model.tradelist;

import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

public class TradeItem
{
	private int _objectId;
	private final Item _item;
	private int _enchant;
	private int _count;
	private int _price;
	
	public TradeItem(ItemInstance item, int count, int price)
	{
		_objectId = item.getObjectId();
		_item = item.getItem();
		_enchant = item.getEnchantLevel();
		_count = count;
		_price = price;
	}
	
	public TradeItem(Item item, int count, int price)
	{
		_objectId = 0;
		_item = item;
		_enchant = 0;
		_count = count;
		_price = price;
	}
	
	public TradeItem(TradeItem item, int count, int price)
	{
		_objectId = item.getObjectId();
		_item = item.getItem();
		_enchant = item.getEnchant();
		_count = count;
		_price = price;
	}
	
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public Item getItem()
	{
		return _item;
	}
	
	public void setEnchant(int enchant)
	{
		_enchant = enchant;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
	
	public void setCount(int count)
	{
		_count = count;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public void setPrice(int price)
	{
		_price = price;
	}
	
	public int getPrice()
	{
		return _price;
	}
}