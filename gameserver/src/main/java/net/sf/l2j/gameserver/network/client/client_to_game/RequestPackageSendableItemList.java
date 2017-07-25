package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.PackageSendableList;

import java.util.List;

public final class RequestPackageSendableItemList extends L2GameClientPacket {
    private int _objectID;

    @Override
    protected void readImpl() {
        _objectID = readD();
    }

    @Override
    public void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }
        List<L2ItemInstance> items = player.getInventory().getAvailableItems(true, false);
        if (items == null) { return; }
        sendPacket(new PackageSendableList(items, _objectID));
    }
}