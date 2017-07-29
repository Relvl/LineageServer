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
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        Henna henna = HennaTable.getInstance().getTemplate(_symbolId);
        if (henna == null) { return; }

        if (!henna.isForThisClass(player)) {
            player.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
            Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " tried to add a forbidden henna.", Config.DEFAULT_PUNISH);
            return;
        }

        if (player.getHennaEmptySlots() == 0) {
            player.sendPacket(SystemMessageId.SYMBOLS_FULL);
            return;
        }

        L2ItemInstance ownedDyes = player.getInventory().getItemByItemId(henna.getDyeId());
        int count = (ownedDyes == null) ? 0 : ownedDyes.getCount();

        if (count < Henna.getAmountDyeRequire()) {
            player.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
            return;
        }

        // reduceAdena sends a message.
        if (!player.getInventory().reduceAdena(EItemProcessPurpose.HENNA, henna.getPrice(), player.getCurrentFolkNPC(), true)) { return; }

        // destroyItemByItemId sends a message.
        if (player.getInventory().destroyItemByItemId(EItemProcessPurpose.HENNA, henna.getDyeId(), Henna.getAmountDyeRequire(), player, player, true) == null) { return; }

        player.addHenna(henna);
    }
}