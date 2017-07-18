package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;

import java.util.List;

public class PrivateStoreListBuy extends L2GameServerPacket {
    private final L2PcInstance _storePlayer;
    private final int _playerAdena;
    private final List<TradeItem> _items;

    public PrivateStoreListBuy(L2PcInstance player, L2PcInstance storePlayer) {
        _storePlayer = storePlayer;
        _storePlayer.getSellList().updateItems();

        _playerAdena = player.getAdena();
        _items = _storePlayer.getBuyList().getAvailableItems(player.getInventory());
    }

    @Override
    protected final void writeImpl() {
        writeC(0xb8);
        writeD(_storePlayer.getObjectId());
        writeD(_playerAdena);
        writeD(_items.size());

        for (TradeItem item : _items) {
            writeD(item.getObjectId());
            writeD(item.getItem().getItemId());
            writeH(item.getEnchant());
            writeD(item.getCount()); // give max possible sell amount

            writeD(item.getItem().getReferencePrice());
            writeH(0);

            writeD(item.getItem().getBodyPart());
            writeH(item.getItem().getType2());
            writeD(item.getPrice());// buyers price

            writeD(item.getCount()); // maximum possible tradecount
        }
    }
}