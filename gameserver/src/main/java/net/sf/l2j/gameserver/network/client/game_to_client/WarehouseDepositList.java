package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.ArrayList;
import java.util.List;

public final class WarehouseDepositList extends L2GameServerPacket {
    public static final int PRIVATE = 1;
    public static final int CLAN = 2;
    public static final int CASTLE = 3; // not sure
    public static final int FREIGHT = 4; // not sure

    private final int _playerAdena;
    private final List<L2ItemInstance> _items;
    private final int _whType;

    public WarehouseDepositList(L2PcInstance player, int type) {
        _whType = type;
        _playerAdena = player.getAdena();
        _items = new ArrayList<>();

        boolean isPrivate = _whType == PRIVATE;
        for (L2ItemInstance temp : player.getInventory().getAvailableItems(true, isPrivate)) {
            if (temp != null && temp.isDepositable(isPrivate)) { _items.add(temp); }
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0x41);
        writeH(_whType);
        writeD(_playerAdena);
        writeH(_items.size());

        for (L2ItemInstance temp : _items) {
            if (temp == null || temp.getItem() == null) { continue; }

            Item item = temp.getItem();

            writeH(item.getType1());
            writeD(temp.getObjectId());
            writeD(temp.getItemId());
            writeD(temp.getCount());
            writeH(item.getType2());
            writeH(temp.getCustomType1());
            writeD(item.getBodyPart());
            writeH(temp.getEnchantLevel());
            writeH(temp.getCustomType2());
            writeH(0x00);
            writeD(temp.getObjectId());
            if (temp.isAugmented()) {
                writeD(0x0000FFFF & temp.getAugmentation().getAugmentationId());
                writeD(temp.getAugmentation().getAugmentationId() >> 16);
            }
            else {
                writeQ(0x00);
            }
        }
        _items.clear();
    }
}