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
package net.sf.l2j.gameserver.network.serverpackets;

import java.util.Collection;
import java.util.Iterator;

import net.sf.l2j.gameserver.model.L2ManufactureItem;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.RecipeList;

/**
 * dd d(dd) d(ddd)
 */
public class RecipeShopManageList extends L2GameServerPacket
{
	private final L2PcInstance _seller;
	private final boolean _isDwarven;
	private Collection<RecipeList> _recipes;
	
	public RecipeShopManageList(L2PcInstance seller, boolean isDwarven)
	{
		_seller = seller;
		_isDwarven = isDwarven;
		
		if (_isDwarven && seller.hasDwarvenCraft())
			_recipes = seller.getDwarvenRecipeBook();
		else
			_recipes = seller.getCommonRecipeBook();
		
		// clean previous recipes
		if (seller.getCreateList() != null)
		{
			final Iterator<L2ManufactureItem> it = seller.getCreateList().getList().iterator();
			while (it.hasNext())
			{
				L2ManufactureItem item = it.next();
				if (item.isDwarven() != _isDwarven || !seller.hasRecipeList(item.getRecipeId()))
					it.remove();
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xd8);
		writeD(_seller.getObjectId());
		writeD(_seller.getAdena());
		writeD(_isDwarven ? 0x00 : 0x01);
		
		if (_recipes == null)
			writeD(0);
		else
		{
			writeD(_recipes.size());// number of items in recipe book
			
			int i = 0;
			for (RecipeList recipe : _recipes)
			{
				writeD(recipe.getId());
				writeD(i + 1);
			}
		}
		
		if (_seller.getCreateList() == null)
			writeD(0);
		else
		{
			L2ManufactureList list = _seller.getCreateList();
			writeD(list.size());
			
			for (L2ManufactureItem item : list.getList())
			{
				writeD(item.getRecipeId());
				writeD(0x00);
				writeD(item.getCost());
			}
		}
	}
}