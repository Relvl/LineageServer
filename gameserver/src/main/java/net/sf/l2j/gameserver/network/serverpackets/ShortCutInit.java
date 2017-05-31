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

import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

public class ShortCutInit extends L2GameServerPacket
{
	private final L2ShortCut[] _shortCuts;
	private final L2PcInstance _activeChar;
	
	public ShortCutInit(L2PcInstance activeChar)
	{
		_activeChar = activeChar;
		_shortCuts = activeChar.getAllShortCuts();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x45);
		writeD(_shortCuts.length);
		
		for (L2ShortCut sc : _shortCuts)
		{
			writeD(sc.getType());
			writeD(sc.getSlot() + sc.getPage() * 12);
			
			switch (sc.getType())
			{
				case L2ShortCut.TYPE_ITEM: // 1
					writeD(sc.getId());
					writeD(sc.getCharacterType());
					writeD(sc.getSharedReuseGroup());
					
					if (sc.getSharedReuseGroup() < 0)
					{
						writeD(0x00); // Remaining time
						writeD(0x00); // Cooldown time
					}
					else
					{
						final ItemInstance item = _activeChar.getInventory().getItemByObjectId(sc.getId());
						if (item == null || !item.isEtcItem())
						{
							writeD(0x00); // Remaining time
							writeD(0x00); // Cooldown time
						}
						else
						{
							final IntIntHolder[] skills = item.getEtcItem().getSkills();
							if (skills == null)
							{
								writeD(0x00); // Remaining time
								writeD(0x00); // Cooldown time
							}
							else
							{
								for (IntIntHolder skillInfo : skills)
								{
									final L2Skill itemSkill = skillInfo.getSkill();
									if (_activeChar.getReuseTimeStamp().containsKey(itemSkill.getReuseHashCode()))
									{
										writeD((int) (_activeChar.getReuseTimeStamp().get(itemSkill.getReuseHashCode()).getRemaining() / 1000L));
										writeD((int) (itemSkill.getReuseDelay() / 1000L));
									}
									else
									{
										writeD(0x00); // Remaining time
										writeD(0x00); // Cooldown time
									}
								}
							}
						}
					}
					
					writeD(0x00); // Augmentation
					break;
				
				case L2ShortCut.TYPE_SKILL: // 2
					writeD(sc.getId());
					writeD(sc.getLevel());
					writeC(0x00); // C5
					writeD(0x01); // C6
					break;
				
				default:
					writeD(sc.getId());
					writeD(0x01); // C6
			}
		}
	}
}