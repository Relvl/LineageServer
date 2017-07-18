package net.sf.l2j.gameserver.model.item.instance;

import net.sf.l2j.gameserver.model.item.EItemModifyState;
import net.sf.l2j.gameserver.model.item.kind.Item;

public class ItemInfo {
    /** Identifier of the L2ItemInstance */
    private int _objectId;

    /** The L2Item template of the L2ItemInstance */
    private Item _item;

    /** The level of enchant on the L2ItemInstance */
    private int _enchant;

    /** The augmentation of the item */
    private int _augmentation;

    /** The quantity of L2ItemInstance */
    private int _count;

    /** The price of the L2ItemInstance */
    private int _price;

    /** The custom L2ItemInstance types (used loto, race tickets) */
    private int _type1;
    private int _type2;

    /** If True the L2ItemInstance is equipped */
    private int _equipped;

    /** The action to do clientside (1=ADD, 2=MODIFY, 3=REMOVE) */
    private EItemModifyState modifyState;

    /** The mana of this item */
    private int _mana;

    /**
     * Get all information from L2ItemInstance to generate ItemInfo.
     *
     * @param item The item instance.
     */
    public ItemInfo(L2ItemInstance item) {
        if (item == null) { return; }

        // Get the Identifier of the L2ItemInstance
        _objectId = item.getObjectId();

        // Get the L2Item of the L2ItemInstance
        _item = item.getItem();

        // Get the enchant level of the L2ItemInstance
        _enchant = item.getEnchantLevel();

        // Get the augmentation boni
        if (item.isAugmented()) { _augmentation = item.getAugmentation().getAugmentationId(); }
        else { _augmentation = 0; }

        // Get the quantity of the L2ItemInstance
        _count = item.getCount();

        // Get custom item types (used loto, race tickets)
        _type1 = item.getCustomType1();
        _type2 = item.getCustomType2();

        // Verify if the L2ItemInstance is equipped
        _equipped = item.isEquipped() ? 1 : 0;
        modifyState = item.getModifyState();

        // Get shadow item mana
        _mana = item.getMana();
    }

    public ItemInfo(L2ItemInstance item, EItemModifyState change) {
        if (item == null) { return; }

        // Get the Identifier of the L2ItemInstance
        _objectId = item.getObjectId();

        // Get the L2Item of the L2ItemInstance
        _item = item.getItem();

        // Get the enchant level of the L2ItemInstance
        _enchant = item.getEnchantLevel();

        // Get the augmentation boni
        if (item.isAugmented()) { _augmentation = item.getAugmentation().getAugmentationId(); }
        else { _augmentation = 0; }

        // Get the quantity of the L2ItemInstance
        _count = item.getCount();

        // Get custom item types (used loto, race tickets)
        _type1 = item.getCustomType1();
        _type2 = item.getCustomType2();

        // Verify if the L2ItemInstance is equipped
        _equipped = item.isEquipped() ? 1 : 0;

        // Get the action to do clientside
        modifyState = change;

        // Get shadow item mana
        _mana = item.getMana();
    }

    public int getObjectId() {
        return _objectId;
    }

    public Item getItem() {
        return _item;
    }

    public int getEnchant() {
        return _enchant;
    }

    public int getAugmentationBoni() {
        return _augmentation;
    }

    public int getCount() {
        return _count;
    }

    public int getPrice() {
        return _price;
    }

    public int getCustomType1() {
        return _type1;
    }

    public int getCustomType2() {
        return _type2;
    }

    public int getEquipped() {
        return _equipped;
    }

    public EItemModifyState getModifyState() {
        return modifyState;
    }

    public int getMana() {
        return _mana;
    }
}