package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;

import java.util.List;

/** Ответ на запрос открытия настройки частной продажи (первый раз, или повторно). */
public class PrivateStoreManageListSell extends L2GameServerPacket {
    private final int _objId;
    private final int _playerAdena;
    private final boolean _packageSale;
    private final List<TradeItem> _itemList;
    private final List<TradeItem> _sellList;

    public PrivateStoreManageListSell(L2PcInstance player, boolean isPackageSale) {
        _objId = player.getObjectId();
        _playerAdena = player.getAdena();

        player.getSellList().updateItems();

        _packageSale = player.getSellList().isPackaged() || isPackageSale;
        _itemList = player.getInventory().getAvailableItems(player.getSellList());
        _sellList = player.getSellList().getItems();
    }

    @Override
    protected final void writeImpl() {
        writeC(0x9a);
        writeD(_objId);
        writeD(_packageSale ? 1 : 0);
        writeD(_playerAdena);

        writeD(_itemList.size());
        for (TradeItem item : _itemList) {
            writeD(item.getItem().getType2());
            writeD(item.getObjectId());
            writeD(item.getItem().getItemId());
            writeD(item.getCount());
            writeH(0x00);
            writeH(item.getEnchant());
            writeH(0x00);
            writeD(item.getItem().getBodyPart());
            writeD(item.getPrice());
        }

        writeD(_sellList.size());
        for (TradeItem item : _sellList) {
            writeD(item.getItem().getType2());
            writeD(item.getObjectId());
            writeD(item.getItem().getItemId());
            writeD(item.getCount());
            writeH(0x00);
            writeH(item.getEnchant());
            writeH(0x00);
            writeD(item.getItem().getBodyPart());
            writeD(item.getPrice());
            writeD(item.getItem().getReferencePrice());
        }
    }
}