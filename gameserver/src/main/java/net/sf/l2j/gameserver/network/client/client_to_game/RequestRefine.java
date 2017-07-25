package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ExVariationResult;
import net.sf.l2j.gameserver.network.client.game_to_client.InventoryUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.StatusUpdate;

public final class RequestRefine extends AbstractRefinePacket {
    private int _targetItemObjId;
    private int _refinerItemObjId;
    private int _gemStoneItemObjId;
    private int _gemStoneCount;

    @Override
    protected void readImpl() {
        _targetItemObjId = readD();
        _refinerItemObjId = readD();
        _gemStoneItemObjId = readD();
        _gemStoneCount = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
        if (targetItem == null) {
            activeChar.sendPacket(new ExVariationResult(0, 0, 0));
            activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
            return;
        }

        L2ItemInstance refinerItem = activeChar.getInventory().getItemByObjectId(_refinerItemObjId);
        if (refinerItem == null) {
            activeChar.sendPacket(new ExVariationResult(0, 0, 0));
            activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
            return;
        }

        L2ItemInstance gemStoneItem = activeChar.getInventory().getItemByObjectId(_gemStoneItemObjId);
        if (gemStoneItem == null) {
            activeChar.sendPacket(new ExVariationResult(0, 0, 0));
            activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
            return;
        }

        if (!isValid(activeChar, targetItem, refinerItem, gemStoneItem)) {
            activeChar.sendPacket(new ExVariationResult(0, 0, 0));
            activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
            return;
        }

        LifeStone ls = getLifeStone(refinerItem.getItemId());
        if (ls == null) {
            activeChar.sendPacket(new ExVariationResult(0, 0, 0));
            activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
            return;
        }

        int lifeStoneLevel = ls.getLevel();
        int lifeStoneGrade = ls.getGrade();
        if (_gemStoneCount != getGemStoneCount(targetItem.getItem().getCrystalType())) {
            activeChar.sendPacket(new ExVariationResult(0, 0, 0));
            activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
            return;
        }

        // unequip item
        if (targetItem.isEquipped()) {
            L2ItemInstance[] unequipped = activeChar.getInventory().unEquipItemInSlotAndRecord(EPaperdollSlot.getByIndex(targetItem.getLocationSlot()));
            InventoryUpdate iu = new InventoryUpdate();

            for (L2ItemInstance itm : unequipped) { iu.addModifiedItem(itm); }
            activeChar.sendPacket(iu);
            activeChar.broadcastUserInfo();
        }

        // Consume the life stone
        if (activeChar.getInventory().destroyItem(EItemProcessPurpose.REQUEST_REFINE, refinerItem, 1, null, false) == null) {
            activeChar.sendPacket(new ExVariationResult(0, 0, 0));
            activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
            return;
        }

        // Consume gemstones
        if (activeChar.getInventory().destroyItem(EItemProcessPurpose.REQUEST_REFINE, gemStoneItem, _gemStoneCount, null, false) == null) {
            activeChar.sendPacket(new ExVariationResult(0, 0, 0));
            activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
            return;
        }

        L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(lifeStoneLevel, lifeStoneGrade);
        targetItem.setAugmentation(aug);

        int stat12 = 0x0000FFFF & aug.getAugmentationId();
        int stat34 = aug.getAugmentationId() >> 16;
        activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));

        InventoryUpdate iu = new InventoryUpdate();
        iu.addModifiedItem(targetItem);
        activeChar.sendPacket(iu);

        StatusUpdate su = new StatusUpdate(activeChar);
        su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
        activeChar.sendPacket(su);
    }
}