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
package net.sf.l2j.gameserver.model.itemcontainer.listeners;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;

public class ItemPassiveSkillsListener implements OnEquipListener
{
	private static ItemPassiveSkillsListener instance = new ItemPassiveSkillsListener();
	
	public static ItemPassiveSkillsListener getInstance()
	{
		return instance;
	}
	
	@Override
	public void onEquip(int slot, ItemInstance item, L2Playable actor)
	{
		final L2PcInstance player = (L2PcInstance) actor;
		final Item it = item.getItem();
		
		boolean update = false;
		boolean updateTimeStamp = false;
		
		if (it instanceof Weapon)
		{
			// Apply augmentation bonuses on equip
			if (item.isAugmented())
				item.getAugmentation().applyBonus(player);
			
			// Verify if the grade penalty is occuring. If yes, then forget +4 dual skills and SA attached to weapon.
			if (player.getExpertiseIndex() < it.getCrystalType().getId())
				return;
			
			// Add skills bestowed from +4 Duals
			if (item.getEnchantLevel() >= 4)
			{
				final L2Skill enchant4Skill = ((Weapon) it).getEnchant4Skill();
				if (enchant4Skill != null)
				{
					player.addSkill(enchant4Skill, false);
					update = true;
				}
			}
		}
		
		final IntIntHolder[] skills = it.getSkills();
		if (skills != null)
		{
			for (IntIntHolder skillInfo : skills)
			{
				if (skillInfo == null)
					continue;
				
				final L2Skill itemSkill = skillInfo.getSkill();
				if (itemSkill != null)
				{
					player.addSkill(itemSkill, false);
					
					if (itemSkill.isActive())
					{
						if (!player.getReuseTimeStamp().containsKey(itemSkill.getReuseHashCode()))
						{
							final int equipDelay = itemSkill.getEquipDelay();
							if (equipDelay > 0)
							{
								player.addTimeStamp(itemSkill, equipDelay);
								player.disableSkill(itemSkill, equipDelay);
							}
						}
						updateTimeStamp = true;
					}
					update = true;
				}
			}
		}
		
		if (update)
		{
			player.sendSkillList();
			
			if (updateTimeStamp)
				player.sendPacket(new SkillCoolTime(player));
		}
	}
	
	@Override
	public void onUnequip(int slot, ItemInstance item, L2Playable actor)
	{
		final L2PcInstance player = (L2PcInstance) actor;
		final Item it = item.getItem();
		
		boolean update = false;
		
		if (it instanceof Weapon)
		{
			// Remove augmentation bonuses on unequip
			if (item.isAugmented())
				item.getAugmentation().removeBonus(player);
			
			// Remove skills bestowed from +4 Duals
			if (item.getEnchantLevel() >= 4)
			{
				final L2Skill enchant4Skill = ((Weapon) it).getEnchant4Skill();
				if (enchant4Skill != null)
				{
					player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
					update = true;
				}
			}
		}
		
		final IntIntHolder[] skills = it.getSkills();
		if (skills != null)
		{
			for (IntIntHolder skillInfo : skills)
			{
				if (skillInfo == null)
					continue;
				
				final L2Skill itemSkill = skillInfo.getSkill();
				if (itemSkill != null)
				{
					boolean found = false;
					
					for (ItemInstance pItem : player.getInventory().getPaperdollItems())
					{
						if (pItem != null && it.getItemId() == pItem.getItemId())
						{
							found = true;
							break;
						}
					}
					
					if (!found)
					{
						player.removeSkill(itemSkill, false, itemSkill.isPassive());
						update = true;
					}
				}
			}
		}
		
		if (update)
			player.sendSkillList();
	}
}