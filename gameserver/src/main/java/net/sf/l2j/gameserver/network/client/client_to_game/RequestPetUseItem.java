package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.datatables.PetDataTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.PetItemList;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public final class RequestPetUseItem extends L2GameClientPacket {
    private int objectId;

    @Override
    protected void readImpl() {
        objectId = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null || !activeChar.hasPet()) { return; }
        L2PetInstance pet = (L2PetInstance) activeChar.getPet();
        L2ItemInstance item = pet.getInventory().getItemByObjectId(objectId);
        if (item == null) { return; }

        if (activeChar.isAlikeDead() || pet.isDead()) {
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(item));
            return;
        }

        if (!item.isEquipped()) {
            if (!item.getItem().checkCondition(pet, pet, true)) { return; }
        }

        // Check if item is pet armor or pet weapon
        if (item.isPetItem()) {
            // Verify if the pet can wear that item
            if (!pet.canWear(item.getItem())) {
                activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
                return;
            }

            if (item.isEquipped()) { pet.getInventory().unEquipItemInSlot(item.getLocationSlot()); }
            else { pet.getInventory().equipPetItem(item); }

            activeChar.sendPacket(new PetItemList(pet));
            pet.updateAndBroadcastStatus(1);
            return;
        }
        else if (PetDataTable.isPetFood(item.getItemId())) {
            if (!pet.canEatFoodId(item.getItemId())) {
                activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
                return;
            }
        }

        // If pet food check is successful or if the item got an handler, use that item.
        IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
        if (handler != null) {
            handler.useItem(pet, item, false);
            pet.updateAndBroadcastStatus(1);
        }
        else { activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM); }

        return;
    }
}