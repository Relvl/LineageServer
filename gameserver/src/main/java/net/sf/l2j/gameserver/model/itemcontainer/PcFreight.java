package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;

import java.util.ArrayList;
import java.util.List;

public class PcFreight extends ItemContainer {
    private final L2PcInstance owner;
    private int activeLocationId;
    private int tempOwnerId;

    public PcFreight(L2PcInstance owner) {
        this.owner = owner;
    }

    @Override
    public String getName() {
        return "Freight";
    }

    @Override
    public EItemProcessPurpose getItemInteractionPurpose() {
        return EItemProcessPurpose.FREIGHT;
    }

    @Override
    public L2PcInstance getOwner() {
        return owner;
    }

    @Override
    public EItemLocation getBaseLocation() {
        return EItemLocation.FREIGHT;
    }

    public void setActiveLocation(int locationId) {
        activeLocationId = locationId;
    }

    /**
     * Returns the quantity of items in the inventory
     *
     * @return int
     */
    @Override
    public int getSize() {
        int size = 0;
        for (L2ItemInstance item : items) {
            if (item.getLocationSlot() == 0 || activeLocationId == 0 || item.getLocationSlot() == activeLocationId) {
                size++;
            }
        }
        return size;
    }

    /**
     * Returns the list of items in inventory
     *
     * @return L2ItemInstance : items in inventory
     */
    @Override
    public List<L2ItemInstance> getItems() {
        List<L2ItemInstance> list = new ArrayList<>();
        for (L2ItemInstance item : items) {
            if (item.getLocationSlot() == 0 || item.getLocationSlot() == activeLocationId) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Returns the item from inventory by using its <B>itemId</B>
     *
     * @param itemId : int designating the ID of the item
     * @return L2ItemInstance designating the item or null if not found in inventory
     */
    @Override
    public L2ItemInstance getItemByItemId(int itemId) {
        for (L2ItemInstance item : items) {
            if (item.getItemId() == itemId && (item.getLocationSlot() == 0 || activeLocationId == 0 || item.getLocationSlot() == activeLocationId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Adds item to PcFreight for further adjustments.
     *
     * @param item : L2ItemInstance to be added from inventory
     */
    @Override
    protected void addItem(L2ItemInstance item) {
        super.addItem(item);
        if (activeLocationId > 0) {
            item.setLocation(item.getLocation(), activeLocationId);
        }
    }

    /**
     * Get back items in PcFreight from database
     */
    @Override
    public void restore() {
        int locationId = activeLocationId;
        activeLocationId = 0;
        super.restore();
        activeLocationId = locationId;
    }

    @Override
    public boolean validateCapacity(int slots) {
        int cap = owner == null ? Config.FREIGHT_SLOTS : owner.getFreightLimit();

        return getSize() + slots <= cap;
    }

    @Override
    public int getOwnerId() {
        if (owner == null) { return tempOwnerId; }
        return super.getOwnerId();
    }

    /**
     * This provides support to load a new PcFreight without owner so that transactions can be done
     *
     * @param val The id of the owner.
     */
    public void doQuickRestore(int val) {
        tempOwnerId = val;
        restore();
    }
}