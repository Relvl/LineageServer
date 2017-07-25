package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.Henna;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.util.Util;

public final class RequestHennaEquip extends L2GameClientPacket {
    private int _symbolId;

    @Override
    protected void readImpl() {
        _symbolId = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        Henna henna = HennaTable.getInstance().getTemplate(_symbolId);
        if (henna == null) { return; }

        if (!henna.isForThisClass(activeChar)) {
            activeChar.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
            Util.handleIllegalPlayerAction(activeChar, activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to add a forbidden henna.", Config.DEFAULT_PUNISH);
            return;
        }

        if (activeChar.getHennaEmptySlots() == 0) {
            activeChar.sendPacket(SystemMessageId.SYMBOLS_FULL);
            return;
        }

        L2ItemInstance ownedDyes = activeChar.getInventory().getItemByItemId(henna.getDyeId());
        int count = (ownedDyes == null) ? 0 : ownedDyes.getCount();

        if (count < Henna.getAmountDyeRequire()) {
            activeChar.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
            return;
        }

        // reduceAdena sends a message.
        if (!activeChar.getInventory().reduceAdena(EItemProcessPurpose.HENNA, henna.getPrice(), activeChar.getCurrentFolkNPC(), true)) { return; }

        // destroyItemByItemId sends a message.
        if (!activeChar.destroyItemByItemId(EItemProcessPurpose.HENNA, henna.getDyeId(), Henna.getAmountDyeRequire(), activeChar, true)) { return; }

        activeChar.addHenna(henna);
    }
}