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
package net.sf.l2j.gameserver.taskmanager;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.OnEquipListener;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Updates the timer and removes the {@link ItemInstance} as a shadow item.
 * @author Hasha
 */
public class ShadowItemTaskManager implements Runnable, OnEquipListener
{
	private static final int DELAY = 1; // 1 second
	
	private final Map<ItemInstance, L2PcInstance> _shadowItems = new ConcurrentHashMap<>();
	
	public static final ShadowItemTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected ShadowItemTaskManager()
	{
		// Run task each second.
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public final void onEquip(int slot, ItemInstance item, L2Playable playable)
	{
		// Must be a shadow item.
		if (!item.isShadowItem())
			return;
		
		// Must be a player.
		if (!(playable instanceof L2PcInstance))
			return;
		
		_shadowItems.put(item, (L2PcInstance) playable);
	}
	
	@Override
	public final void onUnequip(int slot, ItemInstance item, L2Playable actor)
	{
		// Must be a shadow item.
		if (!item.isShadowItem())
			return;
		
		_shadowItems.remove(item);
	}
	
	public final void remove(L2PcInstance player)
	{
		// List is empty, skip.
		if (_shadowItems.isEmpty())
			return;
		
		// Remove ALL associated items.
		_shadowItems.values().removeAll(Collections.singleton(player));
	}
	
	@Override
	public final void run()
	{
		// List is empty, skip.
		if (_shadowItems.isEmpty())
			return;
		
		// For all items.
		for (Entry<ItemInstance, L2PcInstance> entry : _shadowItems.entrySet())
		{
			// Get item and player.
			final ItemInstance item = entry.getKey();
			final L2PcInstance player = entry.getValue();
			
			// Decrease item mana.
			int mana = item.decreaseMana(DELAY);
			
			// If not enough mana, destroy the item and inform the player.
			if (mana == -1)
			{
				// Remove item first.
				player.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				player.sendPacket(iu);
				
				// Destroy shadow item, remove from list.
				player.destroyItem("ShadowItem", item, player, false);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0).addItemName(item.getItemId()));
				_shadowItems.remove(item);
				
				continue;
			}
			
			// Enough mana, show messages.
			if (mana == 60 - DELAY)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1).addItemName(item.getItemId()));
			else if (mana == 300 - DELAY)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5).addItemName(item.getItemId()));
			else if (mana == 600 - DELAY)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10).addItemName(item.getItemId()));
			
			// Update inventory every minute.
			if (mana % 60 == 60 - DELAY)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				player.sendPacket(iu);
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final ShadowItemTaskManager _instance = new ShadowItemTaskManager();
	}
}