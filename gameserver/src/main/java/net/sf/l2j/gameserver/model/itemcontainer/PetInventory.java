package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.world.L2World;

public class PetInventory extends Inventory {
    private final L2PetInstance _owner;

    public PetInventory(L2PetInstance owner) {
        _owner = owner;
    }

    @Override
    public L2PetInstance getOwner() {
        return _owner;
    }

    @Override
    public int getOwnerId() {
        // gets the L2PcInstance-owner's ID
        int id;
        try {
            id = _owner.getOwner().getObjectId();
        }
        catch (NullPointerException e) {
            return 0;
        }
        return id;
    }

    /**
     * Refresh the weight of equipment loaded
     */
    @Override
    protected void refreshWeight() {
        super.refreshWeight();
        _owner.updateAndBroadcastStatus(1);
    }

    public boolean validateCapacity(L2ItemInstance item) {
        int slots = 0;

        if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != EtcItemType.HERB) { slots++; }

        return validateCapacity(slots);
    }

    @Override
    public boolean validateCapacity(int slots) {
        return _items.size() + slots <= _owner.getInventoryLimit();
    }

    public boolean validateWeight(L2ItemInstance item, long count) {
        int weight = 0;

        Item template = ItemTable.getInstance().getTemplate(item.getItemId());
        if (template == null) { return false; }

        weight += count * template.getWeight();
        return validateWeight(weight);
    }

    @Override
    public boolean validateWeight(int weight) {
        return totalWeight + weight <= _owner.getMaxLoad();
    }

    @Override
    protected EItemLocation getBaseLocation() {
        return EItemLocation.PET;
    }

    @Override
    protected EItemLocation getEquipLocation() {
        return EItemLocation.PET_EQUIP;
    }

    @Override
    public void restore() {
        super.restore();

        // check for equipped items from other pets
        for (L2ItemInstance item : _items) {
            if (item.isEquipped()) {
                if (!item.getItem().checkCondition(_owner, _owner, false)) { unEquipItemInSlot(EPaperdollSlot.getByIndex(item.getLocationSlot())); }
            }
        }
    }

    @Override
    public void deleteMe() {
        // Transfer items only if the items list is feeded.
        if (_items != null) {
            // Retrieves the master of the pet owning the inventory.
            L2PcInstance petOwner = _owner.getOwner();
            if (petOwner != null) {
                // Transfer each item to master's inventory.
                for (L2ItemInstance item : _items) {
                    _owner.transferItem(EItemProcessPurpose.RETURN, item.getObjectId(), item.getCount(), petOwner.getInventory(), petOwner, _owner);
                    L2World.getInstance().removeObject(item);
                }
            }
            // Clear the internal inventory items list.
            _items.clear();
        }
    }
}