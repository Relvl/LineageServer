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
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExUseSharedGroupItem;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Template for item skills handler.
 */
public class ItemSkills implements IItemHandler
{
	@Override
	public void useItem(L2Playable playable, ItemInstance item, boolean forceUse)
	{
		if (playable instanceof L2SummonInstance)
			return;
		
		final boolean isPet = playable instanceof L2PetInstance;
		final L2PcInstance activeChar = playable.getActingPlayer();
		
		// Pets can only use tradable items.
		if (isPet && !item.isTradable())
		{
			activeChar.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}
		
		final IntIntHolder[] skills = item.getEtcItem().getSkills();
		if (skills == null)
		{
			_log.info(item.getName() + " does not have registered any skill for handler.");
			return;
		}
		
		for (IntIntHolder skillInfo : skills)
		{
			if (skillInfo == null)
				continue;
			
			final L2Skill itemSkill = skillInfo.getSkill();
			if (itemSkill == null)
				continue;
			
			if (!itemSkill.checkCondition(playable, playable.getTarget(), false))
				return;
			
			// No message on retail, the use is just forgotten.
			if (playable.isSkillDisabled(itemSkill))
				return;
			
			if (!itemSkill.isPotion() && playable.isCastingNow())
				return;
			
			// Item consumption is setup here.
			if (itemSkill.isPotion() || itemSkill.isSimultaneousCast())
			{
				if (!item.isHerb())
				{
					// Normal item consumption is 1, if more, it must be given in DP with getItemConsume().
					if (!playable.destroyItem("Consume", item.getObjectId(), (itemSkill.getItemConsumeId() == 0 && itemSkill.getItemConsume() > 0) ? itemSkill.getItemConsume() : 1, null, false))
					{
						activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
						return;
					}
				}
				
				playable.doSimultaneousCast(itemSkill);
				// Summons should be affected by herbs too, self time effect is handled at L2Effect constructor.
				if (!isPet && item.getItemType() == EtcItemType.HERB && activeChar.hasServitor())
					activeChar.getPet().doSimultaneousCast(itemSkill);
			}
			else
			{
				// Normal item consumption is 1, if more, it must be given in DP with getItemConsume().
				if (!playable.destroyItem("Consume", item.getObjectId(), (itemSkill.getItemConsumeId() == 0 && itemSkill.getItemConsume() > 0) ? itemSkill.getItemConsume() : 1, null, false))
				{
					activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}
				
				playable.getAI().setIntention(CtrlIntention.IDLE);
				if (!playable.useMagic(itemSkill, forceUse, false))
					return;
			}
			
			// Send message to owner.
			if (isPet)
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1).addSkillName(itemSkill));
			else
			{
				final int skillId = skillInfo.getId();
				
				// Buff icon for healing potions.
				switch (skillId)
				{
					case 2031:
					case 2032:
					case 2037:
						final int buffId = activeChar.getShortBuffTaskSkillId();
						
						// Greater healing potions.
						if (skillId == 2037)
							activeChar.shortBuffStatusUpdate(skillId, skillInfo.getValue(), itemSkill.getBuffDuration() / 1000);
						// Healing potions.
						else if (skillId == 2032 && buffId != 2037)
							activeChar.shortBuffStatusUpdate(skillId, skillInfo.getValue(), itemSkill.getBuffDuration() / 1000);
						// Lesser healing potions.
						else
						{
							if (buffId != 2037 && buffId != 2032)
								activeChar.shortBuffStatusUpdate(skillId, skillInfo.getValue(), itemSkill.getBuffDuration() / 1000);
						}
						break;
				}
			}
			
			// Reuse.
			int reuseDelay = itemSkill.getReuseDelay();
			if (item.isEtcItem())
			{
				if (item.getEtcItem().getReuseDelay() > reuseDelay)
					reuseDelay = item.getEtcItem().getReuseDelay();
				
				playable.addTimeStamp(itemSkill, reuseDelay);
				if (reuseDelay != 0)
					playable.disableSkill(itemSkill, reuseDelay);
				
				if (!isPet)
				{
					final int group = item.getEtcItem().getSharedReuseGroup();
					if (group >= 0)
						activeChar.sendPacket(new ExUseSharedGroupItem(item.getItemId(), group, reuseDelay, reuseDelay));
				}
			}
			else if (reuseDelay > 0)
			{
				playable.addTimeStamp(itemSkill, reuseDelay);
				playable.disableSkill(itemSkill, reuseDelay);
			}
		}
	}
}