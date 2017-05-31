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

import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.ArmorSet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

public class ArmorSetListener implements OnEquipListener
{
	private static ArmorSetListener instance = new ArmorSetListener();
	
	public static ArmorSetListener getInstance()
	{
		return instance;
	}
	
	@Override
	public void onEquip(int slot, ItemInstance item, L2Playable actor)
	{
		if (!item.isEquipable())
			return;
		
		final L2PcInstance player = (L2PcInstance) actor;
		
		// Checks if player is wearing a chest item
		final ItemInstance chestItem = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (chestItem == null)
			return;
		
		// checks if there is armorset for chest item that player worns
		final ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
		if (armorSet == null)
			return;
		
		// checks if equipped item is part of set
		if (armorSet.containItem(slot, item.getItemId()))
		{
			if (armorSet.containAll(player))
			{
				L2Skill skill = SkillTable.getInstance().getInfo(armorSet.getSkillId(), 1);
				if (skill != null)
				{
					player.addSkill(SkillTable.getInstance().getInfo(3006, 1), false);
					player.addSkill(skill, false);
					player.sendSkillList();
				}
				
				if (armorSet.containShield(player)) // has shield from set
				{
					L2Skill skills = SkillTable.getInstance().getInfo(armorSet.getShieldSkillId(), 1);
					if (skills != null)
					{
						player.addSkill(skills, false);
						player.sendSkillList();
					}
				}
				
				if (armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
				{
					int skillId = armorSet.getEnchant6skillId();
					if (skillId > 0)
					{
						L2Skill skille = SkillTable.getInstance().getInfo(skillId, 1);
						if (skille != null)
						{
							player.addSkill(skille, false);
							player.sendSkillList();
						}
					}
				}
			}
		}
		else if (armorSet.containShield(item.getItemId()))
		{
			if (armorSet.containAll(player))
			{
				L2Skill skills = SkillTable.getInstance().getInfo(armorSet.getShieldSkillId(), 1);
				if (skills != null)
				{
					player.addSkill(skills, false);
					player.sendSkillList();
				}
			}
		}
	}
	
	@Override
	public void onUnequip(int slot, ItemInstance item, L2Playable actor)
	{
		final L2PcInstance player = (L2PcInstance) actor;
		
		boolean remove = false;
		int removeSkillId1 = 0; // set skill
		int removeSkillId2 = 0; // shield skill
		int removeSkillId3 = 0; // enchant +6 skill
		
		if (slot == Inventory.PAPERDOLL_CHEST)
		{
			final ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(item.getItemId());
			if (armorSet == null)
				return;
			
			remove = true;
			removeSkillId1 = armorSet.getSkillId();
			removeSkillId2 = armorSet.getShieldSkillId();
			removeSkillId3 = armorSet.getEnchant6skillId();
		}
		else
		{
			final ItemInstance chestItem = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if (chestItem == null)
				return;
			
			final ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
			if (armorSet == null)
				return;
			
			if (armorSet.containItem(slot, item.getItemId())) // removed part of set
			{
				remove = true;
				removeSkillId1 = armorSet.getSkillId();
				removeSkillId2 = armorSet.getShieldSkillId();
				removeSkillId3 = armorSet.getEnchant6skillId();
			}
			else if (armorSet.containShield(item.getItemId())) // removed shield
			{
				remove = true;
				removeSkillId2 = armorSet.getShieldSkillId();
			}
		}
		
		if (remove)
		{
			if (removeSkillId1 != 0)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(removeSkillId1, 1);
				if (skill != null)
				{
					player.removeSkill(SkillTable.getInstance().getInfo(3006, 1));
					player.removeSkill(skill);
				}
			}
			
			if (removeSkillId2 != 0)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(removeSkillId2, 1);
				if (skill != null)
					player.removeSkill(skill);
			}
			
			if (removeSkillId3 != 0)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(removeSkillId3, 1);
				if (skill != null)
					player.removeSkill(skill);
			}
			player.sendSkillList();
		}
	}
}