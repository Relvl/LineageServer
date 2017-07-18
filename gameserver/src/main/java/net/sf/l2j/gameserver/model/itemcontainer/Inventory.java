package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemBodyPart;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.OnEquipListener;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.StatsListener;
import net.sf.l2j.gameserver.model.world.L2World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

public abstract class Inventory extends ItemContainer {
    private final Map<EPaperdollSlot, L2ItemInstance> paperdoll = new EnumMap<>(EPaperdollSlot.class);
    private final List<OnEquipListener> paperdollListeners;
    protected int totalWeight;
    private int wornMask;

    protected Inventory() {
        paperdollListeners = new ArrayList<>();
        addPaperdollListener(StatsListener.getInstance());
    }

    public static EPaperdollSlot getPaperdollIndex(EItemBodyPart bodyPart) {
        switch (bodyPart) {
            case SLOT_UNDERWEAR:
                return EPaperdollSlot.PAPERDOLL_UNDER;
            case SLOT_R_EAR:
                return EPaperdollSlot.PAPERDOLL_REAR;
            case SLOT_L_EAR:
                return EPaperdollSlot.PAPERDOLL_LEAR;
            case SLOT_NECK:
                return EPaperdollSlot.PAPERDOLL_NECK;
            case SLOT_R_FINGER:
                return EPaperdollSlot.PAPERDOLL_RFINGER;
            case SLOT_L_FINGER:
                return EPaperdollSlot.PAPERDOLL_LFINGER;
            case SLOT_HEAD:
                return EPaperdollSlot.PAPERDOLL_HEAD;
            case SLOT_R_HAND:
            case SLOT_LR_HAND:
                return EPaperdollSlot.PAPERDOLL_RHAND;
            case SLOT_L_HAND:
                return EPaperdollSlot.PAPERDOLL_LHAND;
            case SLOT_GLOVES:
                return EPaperdollSlot.PAPERDOLL_GLOVES;
            case SLOT_CHEST:
            case SLOT_FULL_ARMOR:
            case SLOT_ALLDRESS:
                return EPaperdollSlot.PAPERDOLL_CHEST;
            case SLOT_LEGS:
                return EPaperdollSlot.PAPERDOLL_LEGS;
            case SLOT_FEET:
                return EPaperdollSlot.PAPERDOLL_FEET;
            case SLOT_BACK:
                return EPaperdollSlot.PAPERDOLL_BACK;
            case SLOT_FACE:
            case SLOT_HAIRALL:
                return EPaperdollSlot.PAPERDOLL_FACE;
            case SLOT_HAIR:
                return EPaperdollSlot.PAPERDOLL_HAIR;
        }
        return null;
    }

    protected abstract ItemLocation getEquipLocation();

    /**
     * Returns the instance of new ChangeRecorder
     *
     * @return ChangeRecorder
     */
    public ChangeRecorder newRecorder() {
        return new ChangeRecorder(this);
    }

    /**
     * Drop item from inventory and updates database
     *
     * @param process   : String Identifier of process triggering this action
     * @param item      : L2ItemInstance to be dropped
     * @param actor     : L2PcInstance Player requesting the item drop
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
     */
    public L2ItemInstance dropItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference) {
        if (item == null) { return null; }

        synchronized (item) {
            if (!_items.contains(item)) { return null; }

            removeItem(item);
            item.setOwnerId(process, 0, actor, reference);
            item.setLocation(ItemLocation.VOID);
            item.setLastChange(L2ItemInstance.REMOVED);

            item.updateDatabase();
            refreshWeight();
        }
        return item;
    }

    /**
     * Drop item from inventory by using its <B>objectID</B> and updates database
     *
     * @param process   : String Identifier of process triggering this action
     * @param objectId  : int Item Instance identifier of the item to be dropped
     * @param count     : int Quantity of items to be dropped
     * @param actor     : L2PcInstance Player requesting the item drop
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
     */
    public L2ItemInstance dropItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference) {
        L2ItemInstance item = getItemByObjectId(objectId);
        if (item == null) { return null; }

        synchronized (item) {
            if (!_items.contains(item)) { return null; }

            // Adjust item quantity and create new instance to drop
            // Directly drop entire item
            if (item.getCount() > count) {
                item.changeCount(process, -count, actor, reference);
                item.setLastChange(L2ItemInstance.MODIFIED);
                item.updateDatabase();

                item = ItemTable.getInstance().createItem(process, item.getItemId(), count, actor, reference);
                item.updateDatabase();
                refreshWeight();
                return item;
            }
        }
        return dropItem(process, item, actor, reference);
    }

    /**
     * Adds item to inventory for further adjustments and Equip it if necessary (itemlocation defined)<BR>
     * <BR>
     *
     * @param item : L2ItemInstance to be added from inventory
     */
    @Override
    protected void addItem(L2ItemInstance item) {
        super.addItem(item);
        if (item.isEquipped()) { equipItem(item); }
    }

    /**
     * Removes item from inventory for further adjustments.
     *
     * @param item : L2ItemInstance to be removed from inventory
     */
    @Override
    protected boolean removeItem(L2ItemInstance item) {
        for (Entry<EPaperdollSlot, L2ItemInstance> entry : paperdoll.entrySet()) {
            if (entry.getValue() == item) {
                unEquipItemInSlot(entry.getKey());
            }
        }
        return super.removeItem(item);
    }

    public L2ItemInstance getPaperdollItem(EPaperdollSlot slot) {
        return paperdoll.get(slot);
    }

    public List<L2ItemInstance> getPaperdollItems() {
        // TODO .values()?
        List<L2ItemInstance> itemsList = new ArrayList<>();
        for (L2ItemInstance itemInstance : paperdoll.values()) {
            if (itemInstance != null) { itemsList.add(itemInstance); }
        }
        return itemsList;
    }

    @Deprecated
    public L2ItemInstance getPaperdollItemByL2ItemId(EItemBodyPart bodyPart) {
        return paperdoll.get(getPaperdollIndex(bodyPart));
    }

    /**
     * Returns the ID of the item in the paperdol slot
     *
     * @param slot : int designating the slot
     * @return int designating the ID of the item
     */
    public int getPaperdollItemId(EPaperdollSlot slot) {
        L2ItemInstance item = paperdoll.get(slot);
        return item != null ? item.getItemId() : 0;
    }

    public int getPaperdollAugmentationId(EPaperdollSlot slot) {
        L2ItemInstance item = paperdoll.get(slot);
        if (item != null) {
            if (item.getAugmentation() != null) {
                return item.getAugmentation().getAugmentationId();
            }
        }
        return 0;
    }

    /**
     * Returns the objectID associated to the item in the paperdoll slot
     *
     * @param slot : int pointing out the slot
     * @return int designating the objectID
     */
    public int getPaperdollObjectId(EPaperdollSlot slot) {
        L2ItemInstance item = paperdoll.get(slot);
        return item != null ? item.getObjectId() : 0;
    }

    /**
     * Adds new inventory's paperdoll listener
     *
     * @param listener PaperdollListener pointing out the listener
     */
    public final synchronized void addPaperdollListener(OnEquipListener listener) {
        assert !paperdollListeners.contains(listener);
        paperdollListeners.add(listener);
    }

    /**
     * Removes a paperdoll listener
     *
     * @param listener PaperdollListener pointing out the listener to be deleted
     */
    public synchronized void removePaperdollListener(OnEquipListener listener) {
        paperdollListeners.remove(listener);
    }

    /**
     * Equips an item in the given slot of the paperdoll. <U><I>Remark :</I></U> The item <B>HAS TO BE</B> already in the inventory
     *
     * @param slot : int pointing out the slot of the paperdoll
     * @param item : L2ItemInstance pointing out the item to add in slot
     * @return L2ItemInstance designating the item placed in the slot before
     */
    public synchronized L2ItemInstance setPaperdollItem(EPaperdollSlot slot, L2ItemInstance item) {
        L2ItemInstance old = paperdoll.get(slot);
        if (old != item) {
            if (old != null) {
                paperdoll.remove(slot);
                // Put old item from paperdoll slot to base location
                old.setLocation(getBaseLocation());
                old.setLastChange(L2ItemInstance.MODIFIED);

                // delete armor mask flag (in case of two-piece armor it does not matter, we need to deactivate mask too)
                wornMask &= ~old.getItem().getItemMask();

                // Notify all paperdoll listener in order to unequip old item in slot
                for (OnEquipListener listener : paperdollListeners) {
                    if (listener == null) { continue; }

                    listener.onUnequip(slot, old, (L2Playable) getOwner());
                }
                old.updateDatabase();
            }
            // Add new item in slot of paperdoll
            if (item != null) {
                paperdoll.put(slot, item);
                item.setLocation(getEquipLocation(), slot.ordinal());
                item.setLastChange(L2ItemInstance.MODIFIED);

                // activate mask (check 2nd armor part for two-piece armors)
                Item armor = item.getItem();
                if (armor.getBodyPart() == EItemBodyPart.SLOT_CHEST) {
                    L2ItemInstance legs = paperdoll.get(EPaperdollSlot.PAPERDOLL_LEGS);
                    if (legs != null && legs.getItem().getItemMask() == armor.getItemMask()) {
                        wornMask |= armor.getItemMask();
                    }
                }
                else if (armor.getBodyPart() == EItemBodyPart.SLOT_LEGS) {
                    L2ItemInstance legs = paperdoll.get(EPaperdollSlot.PAPERDOLL_LEGS);
                    if (legs != null && legs.getItem().getItemMask() == armor.getItemMask()) {
                        wornMask |= armor.getItemMask();
                    }
                }
                else {
                    wornMask |= armor.getItemMask();
                }

                for (OnEquipListener listener : paperdollListeners) {
                    if (listener == null) { continue; }

                    listener.onEquip(slot, item, (L2Playable) getOwner());
                }
                item.updateDatabase();
            }
        }
        return old;
    }

    /**
     * Return the mask of worn item
     *
     * @return int
     */
    public int getWornMask() {
        return wornMask;
    }

    /** @deprecated Заменять надо на более красивый код */
    @Deprecated
    public EItemBodyPart getSlotFromItem(L2ItemInstance item) {
        EItemBodyPart slot = EItemBodyPart.SLOT_NONE;
        EPaperdollSlot location = EPaperdollSlot.getByIndex(item.getLocationSlot());
        switch (location) {
            case PAPERDOLL_UNDER:
                slot = EItemBodyPart.SLOT_UNDERWEAR;
                break;
            case PAPERDOLL_LEAR:
                slot = EItemBodyPart.SLOT_L_EAR;
                break;
            case PAPERDOLL_REAR:
                slot = EItemBodyPart.SLOT_R_EAR;
                break;
            case PAPERDOLL_NECK:
                slot = EItemBodyPart.SLOT_NECK;
                break;
            case PAPERDOLL_RFINGER:
                slot = EItemBodyPart.SLOT_R_FINGER;
                break;
            case PAPERDOLL_LFINGER:
                slot = EItemBodyPart.SLOT_L_FINGER;
                break;
            case PAPERDOLL_HAIR:
                slot = EItemBodyPart.SLOT_HAIR;
                break;
            case PAPERDOLL_FACE:
                slot = EItemBodyPart.SLOT_FACE;
                break;
            case PAPERDOLL_HEAD:
                slot = EItemBodyPart.SLOT_HEAD;
                break;
            case PAPERDOLL_RHAND:
                slot = EItemBodyPart.SLOT_R_HAND;
                break;
            case PAPERDOLL_LHAND:
                slot = EItemBodyPart.SLOT_L_HAND;
                break;
            case PAPERDOLL_GLOVES:
                slot = EItemBodyPart.SLOT_GLOVES;
                break;
            case PAPERDOLL_CHEST:
                slot = item.getItem().getBodyPart();
                break;// fall through
            case PAPERDOLL_LEGS:
                slot = EItemBodyPart.SLOT_LEGS;
                break;
            case PAPERDOLL_BACK:
                slot = EItemBodyPart.SLOT_BACK;
                break;
            case PAPERDOLL_FEET:
                slot = EItemBodyPart.SLOT_FEET;
                break;
        }

        return slot;
    }

    /**
     * Unequips item in body slot and returns alterations.
     *
     * @param slot : int designating the slot of the paperdoll
     * @return L2ItemInstance[] : list of changes
     */
    public L2ItemInstance[] unEquipItemInBodySlotAndRecord(EItemBodyPart slot) {
        ChangeRecorder recorder = newRecorder();

        try {
            unEquipItemInBodySlot(slot);
        }
        finally {
            removePaperdollListener(recorder);
        }
        return recorder.getChangedItems();
    }

    /**
     * Sets item in slot of the paperdoll to null value
     *
     * @param pdollSlot : int designating the slot
     * @return L2ItemInstance designating the item in slot before change
     */
    public L2ItemInstance unEquipItemInSlot(EPaperdollSlot pdollSlot) {
        return setPaperdollItem(pdollSlot, null);
    }

    /**
     * Unepquips item in slot and returns alterations
     *
     * @param slot : int designating the slot
     * @return L2ItemInstance[] : list of items altered
     */
    public L2ItemInstance[] unEquipItemInSlotAndRecord(EPaperdollSlot slot) {
        ChangeRecorder recorder = newRecorder();

        try {
            unEquipItemInSlot(slot);
            if (getOwner() instanceof L2PcInstance) { ((L2PcInstance) getOwner()).refreshExpertisePenalty(); }
        }
        finally {
            removePaperdollListener(recorder);
        }
        return recorder.getChangedItems();
    }

    /**
     * Unequips item in slot (i.e. equips with default value)
     *
     * @param slot : int designating the slot
     * @return the instance of the item.
     */
    public L2ItemInstance unEquipItemInBodySlot(EItemBodyPart slot) {
        EPaperdollSlot pdollSlot = null;

        switch (slot) {
            case SLOT_L_EAR:
                pdollSlot = EPaperdollSlot.PAPERDOLL_LEAR;
                break;
            case SLOT_R_EAR:
                pdollSlot = EPaperdollSlot.PAPERDOLL_REAR;
                break;
            case SLOT_NECK:
                pdollSlot = EPaperdollSlot.PAPERDOLL_NECK;
                break;
            case SLOT_R_FINGER:
                pdollSlot = EPaperdollSlot.PAPERDOLL_RFINGER;
                break;
            case SLOT_L_FINGER:
                pdollSlot = EPaperdollSlot.PAPERDOLL_LFINGER;
                break;
            case SLOT_HAIR:
                pdollSlot = EPaperdollSlot.PAPERDOLL_HAIR;
                break;
            case SLOT_FACE:
                pdollSlot = EPaperdollSlot.PAPERDOLL_FACE;
                break;
            case SLOT_HAIRALL:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_FACE, null);
                pdollSlot = EPaperdollSlot.PAPERDOLL_FACE;
                break;
            case SLOT_HEAD:
                pdollSlot = EPaperdollSlot.PAPERDOLL_HEAD;
                break;
            case SLOT_R_HAND:
            case SLOT_LR_HAND:
                pdollSlot = EPaperdollSlot.PAPERDOLL_RHAND;
                break;
            case SLOT_L_HAND:
                pdollSlot = EPaperdollSlot.PAPERDOLL_LHAND;
                break;
            case SLOT_GLOVES:
                pdollSlot = EPaperdollSlot.PAPERDOLL_GLOVES;
                break;
            case SLOT_CHEST:
            case SLOT_FULL_ARMOR:
            case SLOT_ALLDRESS:
                pdollSlot = EPaperdollSlot.PAPERDOLL_CHEST;
                break;
            case SLOT_LEGS:
                pdollSlot = EPaperdollSlot.PAPERDOLL_LEGS;
                break;
            case SLOT_BACK:
                pdollSlot = EPaperdollSlot.PAPERDOLL_BACK;
                break;
            case SLOT_FEET:
                pdollSlot = EPaperdollSlot.PAPERDOLL_FEET;
                break;
            case SLOT_UNDERWEAR:
                pdollSlot = EPaperdollSlot.PAPERDOLL_UNDER;
                break;
            default:
                _log.info("Unhandled slot type: " + slot);
        }
        if (pdollSlot != null) {
            L2ItemInstance old = setPaperdollItem(pdollSlot, null);
            if (old != null) {
                if (getOwner() instanceof L2PcInstance) { ((L2PcInstance) getOwner()).refreshExpertisePenalty(); }
            }
            return old;
        }
        return null;
    }

    /**
     * Equips item and returns list of alterations<BR>
     * <B>If you dont need return value use {@link Inventory#equipItem(L2ItemInstance)} instead</B>
     *
     * @param item : L2ItemInstance corresponding to the item
     * @return L2ItemInstance[] : list of alterations
     */
    public L2ItemInstance[] equipItemAndRecord(L2ItemInstance item) {
        ChangeRecorder recorder = newRecorder();

        try {
            equipItem(item);
        }
        finally {
            removePaperdollListener(recorder);
        }
        return recorder.getChangedItems();
    }

    /**
     * Equips item in slot of paperdoll.
     *
     * @param item : L2ItemInstance designating the item and slot used.
     */
    public void equipItem(L2ItemInstance item) {
        if (getOwner() instanceof L2PcInstance) {
            // Can't equip item if you are in shop mod or hero item and you're not hero.
            if (((L2PcInstance) getOwner()).isInStoreMode() || (!((L2PcInstance) getOwner()).isHero() && item.isHeroItem())) { return; }
        }

        EItemBodyPart targetSlot = item.getItem().getBodyPart();

        // check if player wear formal
        L2ItemInstance formal = getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
        if (formal != null && formal.getItem().getBodyPart() == EItemBodyPart.SLOT_ALLDRESS) {
            // only chest target can pass this
            switch (targetSlot) {
                case SLOT_LR_HAND:
                case SLOT_L_HAND:
                case SLOT_R_HAND:
                    unEquipItemInBodySlotAndRecord(EItemBodyPart.SLOT_ALLDRESS);
                    break;
                case SLOT_LEGS:
                case SLOT_FEET:
                case SLOT_GLOVES:
                case SLOT_HEAD:
                    return;
            }
        }

        switch (targetSlot) {
            case SLOT_LR_HAND:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND, item);
                break;

            case SLOT_L_HAND:
                L2ItemInstance rh = getPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND);
                if (rh != null && rh.getItem().getBodyPart() == EItemBodyPart.SLOT_LR_HAND && !((rh.getItemType() == WeaponType.BOW && item.getItemType() == EtcItemType.ARROW) || (rh.getItemType() == WeaponType.FISHINGROD && item.getItemType() == EtcItemType.LURE))) {
                    setPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND, null);
                }

                setPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND, item);
                break;

            case SLOT_R_HAND:
                // dont care about arrows, listener will unequip them (hopefully)
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND, item);
                break;

            case SLOT_L_EAR:
            case SLOT_R_EAR:
            case SLOT_LR_EAR:
                if (paperdoll.get(EPaperdollSlot.PAPERDOLL_LEAR) == null) { setPaperdollItem(EPaperdollSlot.PAPERDOLL_LEAR, item); }
                else if (paperdoll.get(EPaperdollSlot.PAPERDOLL_REAR) == null) { setPaperdollItem(EPaperdollSlot.PAPERDOLL_REAR, item); }
                else {
                    if (paperdoll.get(EPaperdollSlot.PAPERDOLL_REAR).getItemId() == item.getItemId()) { setPaperdollItem(EPaperdollSlot.PAPERDOLL_LEAR, item); }
                    else if (paperdoll.get(EPaperdollSlot.PAPERDOLL_LEAR).getItemId() == item.getItemId()) { setPaperdollItem(EPaperdollSlot.PAPERDOLL_REAR, item); }
                    else { setPaperdollItem(EPaperdollSlot.PAPERDOLL_LEAR, item); }
                }
                break;

            case SLOT_L_FINGER:
            case SLOT_R_FINGER:
            case SLOT_LR_FINGER:
                if (paperdoll.get(EPaperdollSlot.PAPERDOLL_LFINGER) == null) {
                    setPaperdollItem(EPaperdollSlot.PAPERDOLL_LFINGER, item);
                }
                else if (paperdoll.get(EPaperdollSlot.PAPERDOLL_RFINGER) == null) {
                    setPaperdollItem(EPaperdollSlot.PAPERDOLL_RFINGER, item);
                }
                else {
                    if (paperdoll.get(EPaperdollSlot.PAPERDOLL_RFINGER).getItemId() == item.getItemId()) {
                        setPaperdollItem(EPaperdollSlot.PAPERDOLL_LFINGER, item);
                    }
                    else if (paperdoll.get(EPaperdollSlot.PAPERDOLL_LFINGER).getItemId() == item.getItemId()) {
                        setPaperdollItem(EPaperdollSlot.PAPERDOLL_RFINGER, item);
                    }
                    else {
                        setPaperdollItem(EPaperdollSlot.PAPERDOLL_LFINGER, item);
                    }
                }
                break;

            case SLOT_NECK:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_NECK, item);
                break;

            case SLOT_FULL_ARMOR:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_LEGS, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST, item);
                break;

            case SLOT_CHEST:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST, item);
                break;

            case SLOT_LEGS:
                // handle full armor
                L2ItemInstance chest = getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
                if (chest != null && chest.getItem().getBodyPart() == EItemBodyPart.SLOT_FULL_ARMOR) {
                    setPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST, null);
                }
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_LEGS, item);
                break;

            case SLOT_FEET:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_FEET, item);
                break;

            case SLOT_GLOVES:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_GLOVES, item);
                break;

            case SLOT_HEAD:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_HEAD, item);
                break;

            case SLOT_FACE:
                L2ItemInstance hair = getPaperdollItem(EPaperdollSlot.PAPERDOLL_HAIR);
                if (hair != null && hair.getItem().getBodyPart() == EItemBodyPart.SLOT_HAIRALL) {
                    setPaperdollItem(EPaperdollSlot.PAPERDOLL_HAIR, null);
                }
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_FACE, item);
                break;

            case SLOT_HAIR:
                L2ItemInstance face = getPaperdollItem(EPaperdollSlot.PAPERDOLL_FACE);
                if (face != null && face.getItem().getBodyPart() == EItemBodyPart.SLOT_HAIRALL) {
                    setPaperdollItem(EPaperdollSlot.PAPERDOLL_FACE, null);
                }
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_HAIR, item);
                break;

            case SLOT_HAIRALL:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_FACE, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_HAIR, item);
                break;

            case SLOT_UNDERWEAR:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_UNDER, item);
                break;

            case SLOT_BACK:
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_BACK, item);
                break;

            case SLOT_ALLDRESS:
                // formal dress
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_LEGS, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_HEAD, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_FEET, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_GLOVES, null);
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST, item);
                break;

            default:
                _log.warning("Unknown body slot " + targetSlot + " for Item ID:" + item.getItemId());
        }
    }

    /**
     * Equips pet item in slot of paperdoll. Concerning pets, armors go to chest location, and weapon to R-hand.
     *
     * @param item : L2ItemInstance designating the item and slot used.
     */
    public void equipPetItem(L2ItemInstance item) {
        if (getOwner() instanceof L2PcInstance) {
            // Can't equip item if you are in shop mod.
            if (((L2PcInstance) getOwner()).isInStoreMode()) { return; }
        }

        // Verify first if item is a pet item.
        if (item.isPetItem()) {
            // Check then about type of item : armor or weapon. Feed the correct slot.
            if (item.getItemType() == WeaponType.PET) {
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND, item);
            }
            else if (item.getItemType() == ArmorType.PET) {
                setPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST, item);
            }
        }
    }

    /**
     * Refresh the weight of equipment loaded
     */
    @Override
    protected void refreshWeight() {
        int weight = 0;

        for (L2ItemInstance item : _items) {
            if (item != null && item.getItem() != null) { weight += item.getItem().getWeight() * item.getCount(); }
        }

        totalWeight = weight;
    }

    /**
     * Returns the totalWeight.
     *
     * @return int
     */
    public int getTotalWeight() {
        return totalWeight;
    }

    /**
     * Return the L2ItemInstance of the arrows needed for this bow.<BR>
     * <BR>
     *
     * @param bow : L2Item designating the bow
     * @return L2ItemInstance pointing out arrows for bow
     */
    public L2ItemInstance findArrowForBow(Item bow) {
        if (bow == null) { return null; }

        int arrowsId = 0;

        switch (bow.getCrystalType()) {
            default:
            case NONE:
                arrowsId = 17;
                break; // Wooden arrow
            case D:
                arrowsId = 1341;
                break; // Bone arrow
            case C:
                arrowsId = 1342;
                break; // Fine steel arrow
            case B:
                arrowsId = 1343;
                break; // Silver arrow
            case A:
                arrowsId = 1344;
                break; // Mithril arrow
            case S:
                arrowsId = 1345;
                break; // Shining arrow
        }

        // Get the L2ItemInstance corresponding to the item identifier and return it
        return getItemByItemId(arrowsId);
    }

    /**
     * Get back items in inventory from database
     */
    @Override
    public void restore() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data");
            statement.setInt(1, getOwnerId());
            statement.setString(2, getBaseLocation().name());
            statement.setString(3, getEquipLocation().name());
            ResultSet inv = statement.executeQuery();

            while (inv.next()) {
                L2ItemInstance item = L2ItemInstance.restoreFromDb(getOwnerId(), inv);
                if (item == null) { continue; }

                if (getOwner() instanceof L2PcInstance) {
                    if (!((L2PcInstance) getOwner()).isHero() && item.isHeroItem()) { item.setLocation(ItemLocation.INVENTORY); }
                }

                L2World.getInstance().addObject(item);

                // If stackable item is found in inventory just add to current quantity
                if (item.isStackable() && getItemByItemId(item.getItemId()) != null) { addItem("Restore", item, getOwner().getActingPlayer(), null); }
                else { addItem(item); }
            }
            inv.close();
            statement.close();
            refreshWeight();
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Could not restore inventory: " + e.getMessage(), e);
        }
    }

    /**
     * Re-notify to paperdoll listeners every equipped item
     */
    public void reloadEquippedItems() {
        for (L2ItemInstance element : paperdoll.values()) {
            if (element == null) { continue; }
            EPaperdollSlot slot = EPaperdollSlot.getByIndex(element.getLocationSlot());
            for (OnEquipListener listener : paperdollListeners) {
                if (listener == null) { continue; }
                listener.onUnequip(slot, element, (L2Playable) getOwner());
                listener.onEquip(slot, element, (L2Playable) getOwner());
            }
        }
    }

    // Recorder of alterations in inventory
    private static final class ChangeRecorder implements OnEquipListener {
        private final Inventory _inventory;
        private final List<L2ItemInstance> _changed;

        /**
         * Constructor of the ChangeRecorder
         *
         * @param inventory
         */
        ChangeRecorder(Inventory inventory) {
            _inventory = inventory;
            _changed = new ArrayList<>();
            _inventory.addPaperdollListener(this);
        }

        /**
         * Add alteration in inventory when item equipped
         */
        @Override
        public void onEquip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor) {
            if (!_changed.contains(item)) { _changed.add(item); }
        }

        /**
         * Add alteration in inventory when item unequipped
         */
        @Override
        public void onUnequip(EPaperdollSlot slot, L2ItemInstance item, L2Playable actor) {
            if (!_changed.contains(item)) { _changed.add(item); }
        }

        /**
         * Returns alterations in inventory
         *
         * @return L2ItemInstance[] : array of alterated items
         */
        public L2ItemInstance[] getChangedItems() {
            return _changed.toArray(new L2ItemInstance[_changed.size()]);
        }
    }
}