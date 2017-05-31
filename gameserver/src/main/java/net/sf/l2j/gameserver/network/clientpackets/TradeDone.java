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

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.tradelist.TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;

public final class TradeDone extends L2GameClientPacket
{
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		final TradeList trade = player.getActiveTradeList();
		if (trade == null)
			return;
		
		if (trade.isLocked())
			return;
		
		if (_response != 1)
		{
			player.cancelActiveTrade();
			return;
		}
		
		// Trade owner not found, or owner is different of packet sender.
		final L2PcInstance owner = trade.getOwner();
		if (owner == null || !owner.equals(player))
			return;
		
		// Trade partner not found, cancel trade
		final L2PcInstance partner = trade.getPartner();
		if (partner == null || L2World.getInstance().getPlayer(partner.getObjectId()) == null)
		{
			player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.cancelActiveTrade();
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			player.cancelActiveTrade();
			return;
		}
		
		// Sender under enchant process, close it.
		if (owner.getActiveEnchantItem() != null)
		{
			owner.setActiveEnchantItem(null);
			owner.sendPacket(EnchantResult.CANCELLED);
			owner.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
		}
		
		// Partner under enchant process, close it.
		if (partner.getActiveEnchantItem() != null)
		{
			partner.setActiveEnchantItem(null);
			partner.sendPacket(EnchantResult.CANCELLED);
			partner.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
		}
		
		trade.confirm();
	}
}