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

import java.util.List;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;
import net.sf.l2j.gameserver.model.tradelist.TradeList;

public class TradeItemUpdate extends L2GameServerPacket
{
	private final List<ItemInstance> _items;
	private final List<TradeItem> _currentTrade;
	
	public TradeItemUpdate(TradeList trade, L2PcInstance activeChar)
	{
		_items = activeChar.getInventory().getItems();
		_currentTrade = trade.getItems();
	}
	
	private int getItemCount(int objectId) // TODO replace for ItemContainer method
	{
		for (ItemInstance item : _items)
			if (item.getObjectId() == objectId)
				return item.getCount();
		
		return 0;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x74);
		writeH(_currentTrade.size());
		
		for (TradeItem item : _currentTrade)
		{
			int availableCount = getItemCount(item.getObjectId()) - item.getCount();
			boolean stackable = item.getItem().isStackable();
			
			if (availableCount == 0)
			{
				availableCount = 1;
				stackable = false;
			}
			
			writeH(stackable ? 3 : 2);
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(availableCount);
			writeH(item.getItem().getType2());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchant());
			writeH(0x00);
			writeH(0x00);
		}
	}
}