package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemBodyPart;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.InventoryUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class RequestUnEquipItem extends L2GameClientPacket {
    private EItemBodyPart bodyPart;

    @Override
    protected void readImpl() {
        bodyPart = EItemBodyPart.getByMask(readD());
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        L2ItemInstance item = activeChar.getInventory().getPaperdollItemByL2ItemId(bodyPart);
        if (item == null) { return; }

        // Prevent of unequiping a cursed weapon
        if (bodyPart == EItemBodyPart.SLOT_LR_HAND && activeChar.isCursedWeaponEquipped()) { return; }

        // Prevent player from unequipping items in special conditions
        if (activeChar.isStunned() || activeChar.isSleeping() || activeChar.isParalyzed() || activeChar.isAfraid() || activeChar.isAlikeDead()) {
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(item));
            return;
        }

        if (activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow()) { return; }

        L2ItemInstance[] unequipped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(bodyPart);

        // show the update in the inventory
        InventoryUpdate iu = new InventoryUpdate();
        for (L2ItemInstance itm : unequipped) {
            itm.unChargeAllShots();
            iu.addModifiedItem(itm);
        }
        activeChar.sendPacket(iu);
        activeChar.broadcastUserInfo();

        // this can be 0 if the user pressed the right mousebutton twice very fast
        if (unequipped.length > 0) {
            SystemMessage sm = null;
            if (unequipped[0].getEnchantLevel() > 0) {
                sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
                sm.addNumber(unequipped[0].getEnchantLevel());
                sm.addItemName(unequipped[0]);
            }
            else {
                sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
                sm.addItemName(unequipped[0]);
            }
            activeChar.sendPacket(sm);
        }
    }
}