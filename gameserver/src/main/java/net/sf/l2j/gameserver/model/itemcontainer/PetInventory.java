package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.PetInventoryUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class PetInventory extends Inventory {
    private final L2PetInstance pet;

    public PetInventory(L2PetInstance owner) {
        pet = owner;
    }

    @Override
    public L2PetInstance getOwner() {
        return pet;
    }

    @Override
    public int getOwnerId() {
        // gets the L2PcInstance-owner's ID
        int id;
        try {
            id = pet.getOwner().getObjectId();
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
        pet.updateAndBroadcastStatus(1);
    }

    public boolean validateCapacity(L2ItemInstance item) {
        int slots = 0;

        if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != EtcItemType.HERB) { slots++; }

        return validateCapacity(slots);
    }

    @Override
    public boolean validateCapacity(int slots) {
        return items.size() + slots <= Config.INVENTORY_MAXIMUM_PET;
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
        return totalWeight + weight <= pet.getMaxLoad();
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
        for (L2ItemInstance item : items) {
            if (item.isEquipped()) {
                if (!item.getItem().checkCondition(pet, pet, false)) { unEquipItemInSlot(EPaperdollSlot.getByIndex(item.getLocationSlot())); }
            }
        }
    }

    @Override
    public void deleteMe() {
        if (items != null) {
            L2PcInstance petOwner = pet.getOwner();
            if (petOwner != null) {
                for (L2ItemInstance item : items) {
                    pet.transferItem(EItemProcessPurpose.RETURN, item.getObjectId(), item.getCount(), petOwner.getInventory(), petOwner, pet);
                    L2World.getInstance().removeObject(item);
                }
            }
            items.clear();
        }
    }

    @Override
    public L2ItemInstance destroyItemByItemId(EItemProcessPurpose process, int itemId, int count, L2PcInstance actor, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = super.destroyItemByItemId(process, itemId, count, actor, reference, sendMessage);
        if (item != null) {
            PetInventoryUpdate petIU = new PetInventoryUpdate();
            petIU.addItem(item);
            pet.sendPacket(petIU);
            if (sendMessage) {
                if (count > 1) { pet.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item.getItemId()).addItemNumber(count)); }
                else { pet.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item.getItemId())); }
            }
        }
        else if (sendMessage) {
            pet.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
        }
        return item;
    }

    @Override
    public L2ItemInstance destroyItem(EItemProcessPurpose process, L2ItemInstance item, int count, L2PcInstance actor, L2Object reference, boolean sendMessage) {
        item = super.destroyItem(process, item, count, actor, reference, sendMessage);
        if (item != null) {
            PetInventoryUpdate petIU = new PetInventoryUpdate();
            petIU.addItem(item);
            pet.sendPacket(petIU);
            if (sendMessage) {
                if (count > 1) { pet.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item.getItemId()).addItemNumber(count)); }
                else { pet.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item.getItemId())); }
            }
        }
        else if (sendMessage) {
            pet.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
        }
        return item;
    }

    @Override
    public L2ItemInstance destroyItem(EItemProcessPurpose process, int objectId, int count, L2PcInstance actor, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = super.destroyItem(process, objectId, count, actor, reference, sendMessage);
        if (item != null) {
            PetInventoryUpdate petIU = new PetInventoryUpdate();
            petIU.addItem(item);
            pet.sendPacket(petIU);
            if (sendMessage) {
                if (count > 1) { pet.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item.getItemId()).addItemNumber(count)); }
                else { pet.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item.getItemId())); }
            }
        }
        else if (sendMessage) {
            pet.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
        }
        return item;
    }

}