package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;

import java.util.List;

public class PrivateStoreManageListBuy extends L2GameServerPacket {
    private final int _objId;
    private final int _playerAdena;
    private final L2ItemInstance[] _itemList;
    private final List<TradeItem> _buyList;

    public PrivateStoreManageListBuy(L2PcInstance player) {
        _objId = player.getObjectId();
        _playerAdena = player.getAdena();
        _itemList = player.getInventory().getUniqueItems(false, true);
        _buyList = player.getBuyList().getItems();
    }

    @Override
    protected final void writeImpl() {
        writeC(0xb7);
        writeD(_objId);
        writeD(_playerAdena);

        writeD(_itemList.length); // inventory items for potential buy
        for (L2ItemInstance item : _itemList) {
            writeD(item.getItemId());
            writeH(item.getEnchantLevel());
            writeD(item.getCount());
            writeD(item.getReferencePrice());
            writeH(0x00);
            writeD(item.getItem().getBodyPart());
            writeH(item.getItem().getType2());
        }

        writeD(_buyList.size()); // count for all items already added for buy
        for (TradeItem item : _buyList) {
            writeD(item.getItem().getItemId());
            writeH(item.getEnchant());
            writeD(item.getCount());
            writeD(item.getItem().getReferencePrice());
            writeH(0x00);
            writeD(item.getItem().getBodyPart());
            writeH(item.getItem().getType2());
            writeD(item.getPrice());// your price
            writeD(item.getItem().getReferencePrice());// fixed store price
        }
    }
}