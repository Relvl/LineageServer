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

public class ListContainer
{
	private int _listId;
	private boolean _applyTaxes = false;
	private boolean _maintainEnchantment = false;
	
	List<Entry> _entries;
	
	public ListContainer()
	{
		_entries = new ArrayList<>();
	}
	
	public void setListId(int listId)
	{
		_listId = listId;
	}
	
	public int getListId()
	{
		return _listId;
	}
	
	public void setApplyTaxes(boolean applyTaxes)
	{
		_applyTaxes = applyTaxes;
	}
	
	public boolean getApplyTaxes()
	{
		return _applyTaxes;
	}
	
	public void setMaintainEnchantment(boolean maintainEnchantment)
	{
		_maintainEnchantment = maintainEnchantment;
	}
	
	public boolean getMaintainEnchantment()
	{
		return _maintainEnchantment;
	}
	
	public void addEntry(Entry e)
	{
		_entries.add(e);
	}
	
	public List<Entry> getEntries()
	{
		return _entries;
	}
}