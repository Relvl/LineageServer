package net.sf.l2j.gameserver.model.itemcontainer.listeners;

import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.ArmorSet;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;

public class ArmorSetListener implements OnEquipListener {
    private static final ArmorSetListener instance = new ArmorSetListener();

    public static ArmorSetListener getInstance() {
        return instance;
    }

    @Override
    public void onEquip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor) {
        if (!item.isEquipable()) { return; }

        L2PcInstance player = (L2PcInstance) actor;

        // Checks if player is wearing a chest item
        L2ItemInstance chestItem = player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
        if (chestItem == null) { return; }

        // checks if there is armorset for chest item that player worns
        ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
        if (armorSet == null) { return; }

        // checks if equipped item is part of set
        if (armorSet.containItem(slot, item.getItemId())) {
            if (armorSet.containAll(player)) {
                L2Skill skill = SkillTable.getInfo(armorSet.getSkillId(), 1);
                if (skill != null) {
                    player.addSkill(SkillTable.getInfo(3006, 1), false);
                    player.addSkill(skill, false);
                    player.sendSkillList();
                }

                if (armorSet.containShield(player)) // has shield from set
                {
                    L2Skill skills = SkillTable.getInfo(armorSet.getShieldSkillId(), 1);
                    if (skills != null) {
                        player.addSkill(skills, false);
                        player.sendSkillList();
                    }
                }

                if (armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
                {
                    int skillId = armorSet.getEnchant6skillId();
                    if (skillId > 0) {
                        L2Skill skille = SkillTable.getInfo(skillId, 1);
                        if (skille != null) {
                            player.addSkill(skille, false);
                            player.sendSkillList();
                        }
                    }
                }
            }
        }
        else if (armorSet.containShield(item.getItemId())) {
            if (armorSet.containAll(player)) {
                L2Skill skills = SkillTable.getInfo(armorSet.getShieldSkillId(), 1);
                if (skills != null) {
                    player.addSkill(skills, false);
                    player.sendSkillList();
                }
            }
        }
    }

    @Override
    public void onUnequip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor) {
        L2PcInstance player = (L2PcInstance) actor;

        boolean remove = false;
        int removeSkillId1 = 0; // set skill
        int removeSkillId2 = 0; // shield skill
        int removeSkillId3 = 0; // enchant +6 skill

        if (slot == EPaperdollSlot.PAPERDOLL_CHEST) {
            ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(item.getItemId());
            if (armorSet == null) { return; }

            remove = true;
            removeSkillId1 = armorSet.getSkillId();
            removeSkillId2 = armorSet.getShieldSkillId();
            removeSkillId3 = armorSet.getEnchant6skillId();
        }
        else {
            L2ItemInstance chestItem = player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
            if (chestItem == null) { return; }

            ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
            if (armorSet == null) { return; }

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

        if (remove) {
            if (removeSkillId1 != 0) {
                L2Skill skill = SkillTable.getInfo(removeSkillId1, 1);
                if (skill != null) {
                    player.removeSkill(SkillTable.getInfo(3006, 1));
                    player.removeSkill(skill);
                }
            }

            if (removeSkillId2 != 0) {
                L2Skill skill = SkillTable.getInfo(removeSkillId2, 1);
                if (skill != null) { player.removeSkill(skill); }
            }

            if (removeSkillId3 != 0) {
                L2Skill skill = SkillTable.getInfo(removeSkillId3, 1);
                if (skill != null) { player.removeSkill(skill); }
            }
            player.sendSkillList();
        }
    }
}