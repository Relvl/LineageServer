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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2FishermanInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MercManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.util.Util;

/**
 * format: cdd (ddd)
 */
public final class RequestSellItem extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private int _listId;
	private IntIntHolder[] _items = null;
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;
		
		_items = new IntIntHolder[count];
		for (int i = 0; i < count; i++)
		{
			int objectId = readD();
			int itemId = readD();
			int cnt = readD();
			if (objectId < 1 || itemId < 1 || cnt < 1)
			{
				_items = null;
				return;
			}
			_items[i] = new IntIntHolder(objectId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
			return;
		
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		// Alt game - Karma punishment
		if (!Config.KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
			return;
		
		L2Npc merchant = null;
		L2Object target = player.getTarget();
		boolean isGoodInstance = (target instanceof L2MerchantInstance || target instanceof L2MercManagerInstance);
		
		merchant = isGoodInstance ? (L2Npc) target : null;
		if (merchant == null || !merchant.canInteract(player))
			return;
		
		if (_listId > 1000000) // lease
		{
			if (merchant.getTemplate().getNpcId() != _listId - 1000000)
				return;
		}
		
		int totalPrice = 0;
		// Proceed the sell
		for (IntIntHolder i : _items)
		{
			ItemInstance item = player.checkItemManipulation(i.getId(), i.getValue());
			if (item == null || (!item.isSellable()))
				continue;
			
			int price = item.getReferencePrice() / 2;
			totalPrice += price * i.getValue();
			if ((Integer.MAX_VALUE / i.getValue()) < price || totalPrice > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", Config.DEFAULT_PUNISH);
				return;
			}
			item = player.getInventory().destroyItem("Sell", i.getId(), i.getValue(), player, merchant);
		}
		
		player.addAdena("Sell", totalPrice, merchant, false);
		
		// Send the htm, if existing.
		String htmlFolder = "";
		if (merchant instanceof L2MerchantInstance)
			htmlFolder = "merchant";
		else if (merchant instanceof L2FishermanInstance)
			htmlFolder = "fisherman";
		
		if (!htmlFolder.isEmpty())
		{
			final String content = HtmCache.getInstance().getHtm("data/html/" + htmlFolder + "/" + merchant.getNpcId() + "-sold.htm");
			if (content != null)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(merchant.getObjectId());
				html.setHtml(content);
				html.replace("%objectId%", merchant.getObjectId());
				player.sendPacket(html);
			}
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ItemList(player, true));
	}
}