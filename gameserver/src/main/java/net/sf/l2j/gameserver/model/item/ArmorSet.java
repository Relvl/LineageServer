package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

public final class ArmorSet {
    private final int[] _set;
    private final int _skillId;

    private final int _shield;
    private final int _shieldSkillId;

    private final int _enchant6Skill;

    public ArmorSet(int[] set, int skillId, int shield, int shieldSkillId, int enchant6Skill) {
        _set = set;
        _skillId = skillId;

        _shield = shield;
        _shieldSkillId = shieldSkillId;

        _enchant6Skill = enchant6Skill;
    }

    /**
     * Checks if player have equipped all items from set (not checking shield)
     *
     * @param player whose inventory is being checked
     * @return True if player equips whole set
     */
    public boolean containAll(L2PcInstance player) {
        final Inventory inv = player.getInventory();

        int legs = 0;
        int head = 0;
        int gloves = 0;
        int feet = 0;

        final L2ItemInstance legsItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LEGS);
        if (legsItem != null) { legs = legsItem.getItemId(); }

        if (_set[1] != 0 && _set[1] != legs) { return false; }

        final L2ItemInstance headItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_HEAD);
        if (headItem != null) { head = headItem.getItemId(); }

        if (_set[2] != 0 && _set[2] != head) { return false; }

        final L2ItemInstance glovesItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_GLOVES);
        if (glovesItem != null) { gloves = glovesItem.getItemId(); }

        if (_set[3] != 0 && _set[3] != gloves) { return false; }

        final L2ItemInstance feetItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_FEET);
        if (feetItem != null) { feet = feetItem.getItemId(); }

        if (_set[4] != 0 && _set[4] != feet) { return false; }

        return true;
    }

    public boolean containItem(EPaperdollSlot slot, int itemId) {
        switch (slot) {
            case PAPERDOLL_CHEST:
                return _set[0] == itemId;

            case PAPERDOLL_LEGS:
                return _set[1] == itemId;

            case PAPERDOLL_HEAD:
                return _set[2] == itemId;

            case PAPERDOLL_GLOVES:
                return _set[3] == itemId;

            case PAPERDOLL_FEET:
                return _set[4] == itemId;

            default:
                return false;
        }
    }

    public int getSkillId() {
        return _skillId;
    }

    public boolean containShield(L2PcInstance player) {
        final L2ItemInstance shieldItem = player.getInventory().getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND);
        if (shieldItem != null && shieldItem.getItemId() == _shield) { return true; }

        return false;
    }

    public boolean containShield(int shieldId) {
        if (_shield == 0) { return false; }

        return _shield == shieldId;
    }

    public int getShieldSkillId() {
        return _shieldSkillId;
    }

    public int getEnchant6skillId() {
        return _enchant6Skill;
    }

    /**
     * Checks if all parts of set are enchanted to +6 or more
     *
     * @param player
     * @return
     */
    public boolean isEnchanted6(L2PcInstance player) {
        final Inventory inv = player.getInventory();

        final L2ItemInstance chestItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
        if (chestItem.getEnchantLevel() < 6) { return false; }

        int legs = 0;
        int head = 0;
        int gloves = 0;
        int feet = 0;

        final L2ItemInstance legsItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LEGS);
        if (legsItem != null && legsItem.getEnchantLevel() > 5) { legs = legsItem.getItemId(); }

        if (_set[1] != 0 && _set[1] != legs) { return false; }

        final L2ItemInstance headItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_HEAD);
        if (headItem != null && headItem.getEnchantLevel() > 5) { head = headItem.getItemId(); }

        if (_set[2] != 0 && _set[2] != head) { return false; }

        final L2ItemInstance glovesItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_GLOVES);
        if (glovesItem != null && glovesItem.getEnchantLevel() > 5) { gloves = glovesItem.getItemId(); }

        if (_set[3] != 0 && _set[3] != gloves) { return false; }

        final L2ItemInstance feetItem = inv.getPaperdollItem(EPaperdollSlot.PAPERDOLL_FEET);
        if (feetItem != null && feetItem.getEnchantLevel() > 5) { feet = feetItem.getItemId(); }

        if (_set[4] != 0 && _set[4] != feet) { return false; }

        return true;
    }

    /**
     * @return chest, legs, gloves, feet, head
     */
    public int[] getSetItemsId() {
        return _set;
    }

    /**
     * @return shield id
     */
    public int getShield() {
        return _shield;
    }
}