package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.ArmorSet;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.util.Util;

public final class RequestEnchantItem extends AbstractEnchantPacket {
    private int _objectId;

    @Override
    protected void readImpl() {
        _objectId = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null || _objectId == 0) { return; }

        if (!activeChar.isOnline() || getClient().isDetached()) {
            activeChar.setActiveEnchantItem(null);
            return;
        }

        if (activeChar.isProcessingTransaction() || activeChar.isInStoreMode()) {
            activeChar.sendPacket(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE);
            activeChar.setActiveEnchantItem(null);
            activeChar.sendPacket(EnchantResult.CANCELLED);
            return;
        }

        L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
        L2ItemInstance scroll = activeChar.getActiveEnchantItem();

        if (item == null || scroll == null) {
            activeChar.setActiveEnchantItem(null);
            activeChar.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
            activeChar.sendPacket(EnchantResult.CANCELLED);
            return;
        }

        // template for scroll
        EnchantScroll scrollTemplate = getEnchantScroll(scroll);
        if (scrollTemplate == null) { return; }

        // first validation check
        if (!scrollTemplate.isValid(item) || !isEnchantable(item)) {
            activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
            activeChar.setActiveEnchantItem(null);
            activeChar.sendPacket(EnchantResult.CANCELLED);
            return;
        }

        // attempting to destroy scroll
        scroll = activeChar.getInventory().destroyItem(EItemProcessPurpose.ENCHANT, scroll.getObjectId(), 1, activeChar, item, true);
        if (scroll == null) {
            Util.handleIllegalPlayerAction(activeChar, activeChar.getName() + " tried to enchant without scroll.", Config.DEFAULT_PUNISH);
            activeChar.setActiveEnchantItem(null);
            activeChar.sendPacket(EnchantResult.CANCELLED);
            return;
        }

        if (activeChar.getActiveTradeList() != null) {
            activeChar.cancelActiveTrade();
            activeChar.sendPacket(SystemMessageId.TRADE_ATTEMPT_FAILED);
            return;
        }

        synchronized (item) {
            double chance = scrollTemplate.getChance(item);

            // last validation check
            if (item.getOwnerId() != activeChar.getObjectId() || !isEnchantable(item) || chance < 0) {
                activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
                activeChar.setActiveEnchantItem(null);
                activeChar.sendPacket(EnchantResult.CANCELLED);
                return;
            }

            // success
            if (Rnd.nextDouble() < chance) {
                // announce the success
                SystemMessage sm;

                if (item.getEnchantLevel() == 0) {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED);
                    sm.addItemName(item.getItemId());
                    activeChar.sendPacket(sm);
                }
                else {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED);
                    sm.addNumber(item.getEnchantLevel());
                    sm.addItemName(item.getItemId());
                    activeChar.sendPacket(sm);
                }

                item.setEnchantLevel(item.getEnchantLevel() + 1);
                item.updateDatabase();

                // If item is equipped, verify the skill obtention (+4 duals, +6 armorset).
                if (item.isEquipped()) {
                    Item it = item.getItem();

                    // Add skill bestowed by +4 duals.
                    if (it instanceof Weapon && item.getEnchantLevel() == 4) {
                        L2Skill enchant4Skill = ((Weapon) it).getEnchant4Skill();
                        if (enchant4Skill != null) {
                            activeChar.addSkill(enchant4Skill, false);
                            activeChar.sendSkillList();
                        }
                    }
                    // Add skill bestowed by +6 armorset.
                    else if (it instanceof Armor && item.getEnchantLevel() == 6) {
                        // Checks if player is wearing a chest item
                        L2ItemInstance chestItem = activeChar.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
                        if (chestItem != null) {
                            ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
                            if (armorSet != null && armorSet.isEnchanted6(activeChar)) // has all parts of set enchanted to 6 or more
                            {
                                int skillId = armorSet.getEnchant6skillId();
                                if (skillId > 0) {
                                    L2Skill skill = SkillTable.getInfo(skillId, 1);
                                    if (skill != null) {
                                        activeChar.addSkill(skill, false);
                                        activeChar.sendSkillList();
                                    }
                                }
                            }
                        }
                    }
                }
                activeChar.sendPacket(EnchantResult.SUCCESS);
            }
            else {
                // Drop passive skills from items.
                if (item.isEquipped()) {
                    Item it = item.getItem();

                    // Remove skill bestowed by +4 duals.
                    if (it instanceof Weapon && item.getEnchantLevel() >= 4) {
                        L2Skill enchant4Skill = ((Weapon) it).getEnchant4Skill();
                        if (enchant4Skill != null) {
                            activeChar.removeSkill(enchant4Skill, false);
                            activeChar.sendSkillList();
                        }
                    }
                    // Add skill bestowed by +6 armorset.
                    else if (it instanceof Armor && item.getEnchantLevel() >= 6) {
                        // Checks if player is wearing a chest item
                        L2ItemInstance chestItem = activeChar.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
                        if (chestItem != null) {
                            ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
                            if (armorSet != null && armorSet.isEnchanted6(activeChar)) // has all parts of set enchanted to 6 or more
                            {
                                int skillId = armorSet.getEnchant6skillId();
                                if (skillId > 0) {
                                    L2Skill skill = SkillTable.getInfo(skillId, 1);
                                    if (skill != null) {
                                        activeChar.removeSkill(skill, false);
                                        activeChar.sendSkillList();
                                    }
                                }
                            }
                        }
                    }
                }

                if (scrollTemplate.isBlessed()) {
                    // blessed enchant - clear enchant value
                    activeChar.sendPacket(SystemMessageId.BLESSED_ENCHANT_FAILED);

                    item.setEnchantLevel(0);
                    item.updateDatabase();
                    activeChar.sendPacket(EnchantResult.UNSUCCESS);
                }
                else {
                    // enchant failed, destroy item
                    int crystalId = item.getItem().getCrystalItemId();
                    int count = item.getCrystalCount() - (item.getItem().getCrystalCount() + 1) / 2;
                    if (count < 1) { count = 1; }

                    L2ItemInstance destroyItem = activeChar.getInventory().destroyItem(EItemProcessPurpose.ENCHANT, item, item.getCount(), activeChar, true);
                    if (destroyItem == null) {
                        // unable to destroy item, cheater ?
                        Util.handleIllegalPlayerAction(activeChar, "Unable to delete item on enchant failure from player " + activeChar.getName() + ", possible cheater !", Config.DEFAULT_PUNISH);
                        activeChar.setActiveEnchantItem(null);
                        activeChar.sendPacket(EnchantResult.CANCELLED);
                        return;
                    }

                    if (crystalId != 0) {
                        activeChar.getInventory().addItem(EItemProcessPurpose.ENCHANT, crystalId, count, activeChar, destroyItem);
                        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(crystalId).addItemNumber(count));
                    }

                    InventoryUpdate iu = new InventoryUpdate();
                    if (destroyItem.getCount() == 0) { iu.addRemovedItem(destroyItem); }
                    else { iu.addModifiedItem(destroyItem); }

                    activeChar.sendPacket(iu);

                    // Messages.
                    if (item.getEnchantLevel() > 0) { activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_S2_EVAPORATED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId())); }
                    else { activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_EVAPORATED).addItemName(item.getItemId())); }

                    L2World.getInstance().removeObject(destroyItem);
                    if (crystalId == 0) { activeChar.sendPacket(EnchantResult.UNK_RESULT_4); }
                    else { activeChar.sendPacket(EnchantResult.UNK_RESULT_1); }

                    StatusUpdate su = new StatusUpdate(activeChar);
                    su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
                    activeChar.sendPacket(su);
                }
            }

            activeChar.sendPacket(new ItemList(activeChar, false));
            activeChar.broadcastUserInfo();
            activeChar.setActiveEnchantItem(null);
        }
    }
}