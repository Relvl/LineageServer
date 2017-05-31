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
package net.sf.l2j.gameserver.model.buylist;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class NpcBuyList
{
	private final int _listId;
	private final Map<Integer, Product> _products = new LinkedHashMap<>();
	private int _npcId;
	
	public NpcBuyList(int listId)
	{
		_listId = listId;
	}
	
	public int getListId()
	{
		return _listId;
	}
	
	public Collection<Product> getProducts()
	{
		return _products.values();
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public void setNpcId(int id)
	{
		_npcId = id;
	}
	
	public Product getProductByItemId(int itemId)
	{
		return _products.get(itemId);
	}
	
	public void addProduct(Product product)
	{
		_products.put(product.getItemId(), product);
	}
	
	public boolean isNpcAllowed(int npcId)
	{
		return _npcId == npcId;
	}
}