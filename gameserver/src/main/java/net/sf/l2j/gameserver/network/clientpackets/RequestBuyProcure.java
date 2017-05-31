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
package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2ManorManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public class RequestBuyProcure extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 8; // length of the one item
	
	@SuppressWarnings("unused")
	private int _listId;
	private Procure[] _items = null;
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		
		int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;
		
		_items = new Procure[count];
		for (int i = 0; i < count; i++)
		{
			readD(); // service
			int itemId = readD();
			int cnt = readD();
			if (itemId < 1 || cnt < 1)
			{
				_items = null;
				return;
			}
			_items[i] = new Procure(itemId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (!FloodProtectors.performAction(getClient(), Action.MANOR))
			return;
		
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (_items == null)
			return;
		
		// Alt game - Karma punishment
		if (!Config.KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
			return;
		
		L2Object manager = player.getCurrentFolkNPC();
		if (!(manager instanceof L2ManorManagerInstance))
			return;
		
		if (!player.isInsideRadius(manager, INTERACTION_DISTANCE, true, false))
			return;
		
		Castle castle = ((L2ManorManagerInstance) manager).getCastle();
		int slots = 0;
		int weight = 0;
		
		for (Procure i : _items)
		{
			i.setReward(castle);
			
			Item template = ItemTable.getInstance().getTemplate(i.getReward());
			weight += i.getCount() * template.getWeight();
			
			if (!template.isStackable())
				slots += i.getCount();
			else if (player.getInventory().getItemByItemId(i.getItemId()) == null)
				slots++;
		}
		
		if (!player.getInventory().validateWeight(weight))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}
		
		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}
		
		// Proceed the purchase
		InventoryUpdate playerIU = new InventoryUpdate();
		
		for (Procure i : _items)
		{
			// check if player have correct items count
			ItemInstance item = player.getInventory().getItemByItemId(i.getItemId());
			if (item == null || item.getCount() < i.getCount())
				continue;
			
			ItemInstance iteme = player.getInventory().destroyItemByItemId("Manor", i.getItemId(), i.getCount(), player, manager);
			if (iteme == null)
				continue;
			
			// Add item to Inventory and adjust update packet
			item = player.getInventory().addItem("Manor", i.getReward(), i.getCount(), player, manager);
			if (item == null)
				continue;
			
			playerIU.addRemovedItem(iteme);
			if (item.getCount() > i.getCount())
				playerIU.addModifiedItem(item);
			else
				playerIU.addNewItem(item);
			
			// Send Char Buy Messages
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(item.getItemId());
			sm.addItemNumber(i.getCount());
			player.sendPacket(sm);
		}
		
		// Send update packets
		player.sendPacket(playerIU);
		
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
	
	private static class Procure
	{
		private final int _itemId;
		private final int _count;
		private int _reward;
		
		public Procure(int id, int num)
		{
			_itemId = id;
			_count = num;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public int getCount()
		{
			return _count;
		}
		
		public int getReward()
		{
			return _reward;
		}
		
		public void setReward(Castle c)
		{
			_reward = L2Manor.getInstance().getRewardItem(_itemId, c.getCrop(_itemId, CastleManorManager.PERIOD_CURRENT).getReward());
		}
	}
}