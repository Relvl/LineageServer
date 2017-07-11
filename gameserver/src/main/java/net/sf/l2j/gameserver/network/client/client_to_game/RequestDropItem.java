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
package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.InventoryUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.ItemList;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;

public final class RequestDropItem extends L2GameClientPacket
{
	private int _objectId;
	private int _count;
	private int _x;
	private int _y;
	private int _z;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (!FloodProtectors.performAction(getClient(), Action.DROP_ITEM))
			return;
		
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || activeChar.isDead())
			return;
		
		final ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		if (item == null || _count == 0 || !activeChar.validateItemManipulation(_objectId) || (!Config.ALLOW_DISCARDITEM && !activeChar.isGM()) || !item.isDropable())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		
		if (item.isQuestItem())
			return;
		
		if (_count > item.getCount())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		
		if (_count < 0)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestDropItem] count <= 0! ban! oid: " + _objectId + " owner: " + activeChar.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		
		if (!item.isStackable() && _count > 1)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestDropItem] count > 1 but item is not stackable! ban! oid: " + _objectId + " owner: " + activeChar.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		
		if (!activeChar.getAccessLevel().allowTransaction())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		if (activeChar.isProcessingTransaction() || activeChar.isInStoreMode())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}
		
		if (activeChar.isFishing())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
			return;
		}
		
		if (item.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.AUGMENTED_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		if (activeChar.isCastingNow())
		{
			if (activeChar.getCurrentSkill().getSkill() != null && activeChar.getCurrentSkill().getSkill().getItemConsumeId() == item.getItemId())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}
		
		if (activeChar.isCastingSimultaneouslyNow())
		{
			if (activeChar.getLastSimultaneousSkillCast() != null && activeChar.getLastSimultaneousSkillCast().getItemConsumeId() == item.getItemId())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}
		
		if (Item.TYPE2_QUEST == item.getItem().getType2() && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_EXCHANGE_ITEM);
			return;
		}
		
		if (!activeChar.isInsideRadius(_x, _y, 150, false) || Math.abs(_z - activeChar.getZ()) > 50)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_DISTANCE_TOO_FAR);
			return;
		}
		
		if (item.isEquipped() && (!item.isStackable() || (item.isStackable() && _count >= item.getCount())))
		{
			ItemInstance[] unequipped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance itm : unequipped)
			{
				itm.unChargeAllShots();
				iu.addModifiedItem(itm);
			}
			
			activeChar.sendPacket(iu);
			activeChar.broadcastUserInfo();
			activeChar.sendPacket(new ItemList(activeChar, true));
		}
		
		activeChar.dropItem("Drop", _objectId, _count, _x, _y, _z, null, false, false);
	}
}