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
package net.sf.l2j.gameserver.model.multisell;

import java.util.ArrayList;
import java.util.List;

public class Entry
{
	private int _entryId;
	
	private final List<Ingredient> _products = new ArrayList<>();
	private final List<Ingredient> _ingredients = new ArrayList<>();
	
	public void setEntryId(int entryId)
	{
		_entryId = entryId;
	}
	
	public int getEntryId()
	{
		return _entryId;
	}
	
	public void addProduct(Ingredient product)
	{
		_products.add(product);
	}
	
	public List<Ingredient> getProducts()
	{
		return _products;
	}
	
	public void addIngredient(Ingredient ingredient)
	{
		_ingredients.add(ingredient);
	}
	
	public List<Ingredient> getIngredients()
	{
		return _ingredients;
	}
}