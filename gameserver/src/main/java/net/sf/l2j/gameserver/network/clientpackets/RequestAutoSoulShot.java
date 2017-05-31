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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestAutoSoulShot extends L2GameClientPacket
{
	private int _itemId;
	private int _type; // 1 = on : 0 = off;
	
	@Override
	protected void readImpl()
	{
		_itemId = readD();
		_type = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		if (!activeChar.isInStoreMode() && activeChar.getActiveRequester() == null && !activeChar.isDead())
		{
			ItemInstance item = activeChar.getInventory().getItemByItemId(_itemId);
			if (item == null)
				return;
			
			if (_type == 1)
			{
				// Fishingshots are not automatic on retail
				if (_itemId < 6535 || _itemId > 6540)
				{
					// Attempt to charge first shot on activation
					if (_itemId == 6645 || _itemId == 6646 || _itemId == 6647)
					{
						if (activeChar.getPet() != null)
						{
							// Cannot activate bss automation during Olympiad.
							if (_itemId == 6647 && activeChar.isInOlympiadMode())
							{
								activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
								return;
							}
							
							if (_itemId == 6645)
							{
								if (activeChar.getPet().getSoulShotsPerHit() > item.getCount())
								{
									activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS_FOR_PET);
									return;
								}
							}
							else
							{
								if (activeChar.getPet().getSpiritShotsPerHit() > item.getCount())
								{
									activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS_FOR_PET);
									return;
								}
							}
							
							// start the auto soulshot use
							activeChar.addAutoSoulShot(_itemId);
							activeChar.sendPacket(new ExAutoSoulShot(_itemId, _type));
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO).addItemName(_itemId));
							activeChar.rechargeShots(true, true);
							activeChar.getPet().rechargeShots(true, true);
						}
						else
							activeChar.sendPacket(SystemMessageId.NO_SERVITOR_CANNOT_AUTOMATE_USE);
					}
					else
					{
						// Cannot activate bss automation during Olympiad.
						if (_itemId >= 3947 && _itemId <= 3952 && activeChar.isInOlympiadMode())
						{
							activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
							return;
						}
						
						// Activate the visual effect
						activeChar.addAutoSoulShot(_itemId);
						activeChar.sendPacket(new ExAutoSoulShot(_itemId, _type));
						
						// start the auto soulshot use
						if (activeChar.getActiveWeaponItem() != activeChar.getFistsWeaponItem() && item.getItem().getCrystalType() == activeChar.getActiveWeaponItem().getCrystalType())
							activeChar.rechargeShots(true, true);
						else
						{
							if ((_itemId >= 2509 && _itemId <= 2514) || (_itemId >= 3947 && _itemId <= 3952) || _itemId == 5790)
								activeChar.sendPacket(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH);
							else
								activeChar.sendPacket(SystemMessageId.SOULSHOTS_GRADE_MISMATCH);
						}
						
						// In both cases (match/mismatch), that message is displayed.
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO).addItemName(_itemId));
					}
				}
			}
			else if (_type == 0)
			{
				// cancel the auto soulshot use
				activeChar.removeAutoSoulShot(_itemId);
				activeChar.sendPacket(new ExAutoSoulShot(_itemId, _type));
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addItemName(_itemId));
			}
		}
	}
}