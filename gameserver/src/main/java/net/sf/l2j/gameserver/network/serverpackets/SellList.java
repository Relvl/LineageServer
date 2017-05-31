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

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

public class SellList extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;
	private final int _money;
	private final List<ItemInstance> _selllist = new ArrayList<>();
	
	public SellList(L2PcInstance player)
	{
		_activeChar = player;
		_money = _activeChar.getAdena();
	}
	
	@Override
	public final void runImpl()
	{
		for (ItemInstance item : _activeChar.getInventory().getItems())
		{
			if (!item.isEquipped() && item.isSellable() && (_activeChar.getPet() == null || item.getObjectId() != _activeChar.getPet().getControlItemId()))
				_selllist.add(item);
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x10);
		writeD(_money);
		writeD(0x00);
		writeH(_selllist.size());
		
		for (ItemInstance temp : _selllist)
		{
			if (temp == null || temp.getItem() == null)
				continue;
			
			Item item = temp.getItem();
			
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(item.getType2());
			writeH(temp.getCustomType1());
			writeD(item.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			writeH(0x00);
			writeD(item.getReferencePrice() / 2);
		}
	}
}