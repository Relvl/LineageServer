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
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.Henna;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.util.Util;

public final class RequestHennaEquip extends L2GameClientPacket
{
	private int _symbolId;
	
	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		final Henna henna = HennaTable.getInstance().getTemplate(_symbolId);
		if (henna == null)
			return;
		
		if (!henna.isForThisClass(activeChar))
		{
			activeChar.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
			Util.handleIllegalPlayerAction(activeChar, activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to add a forbidden henna.", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (activeChar.getHennaEmptySlots() == 0)
		{
			activeChar.sendPacket(SystemMessageId.SYMBOLS_FULL);
			return;
		}
		
		final ItemInstance ownedDyes = activeChar.getInventory().getItemByItemId(henna.getDyeId());
		final int count = (ownedDyes == null) ? 0 : ownedDyes.getCount();
		
		if (count < Henna.getAmountDyeRequire())
		{
			activeChar.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
			return;
		}
		
		// reduceAdena sends a message.
		if (!activeChar.reduceAdena("Henna", henna.getPrice(), activeChar.getCurrentFolkNPC(), true))
			return;
		
		// destroyItemByItemId sends a message.
		if (!activeChar.destroyItemByItemId("Henna", henna.getDyeId(), Henna.getAmountDyeRequire(), activeChar, true))
			return;
		
		activeChar.addHenna(henna);
	}
}