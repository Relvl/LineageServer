package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.List;

public class WarehouseWithdrawList extends L2GameServerPacket {
    public static final int PRIVATE = 1;
    public static final int CLAN = 2;
    public static final int CASTLE = 3; // not sure
    public static final int FREIGHT = 4; // not sure

    private int _whType;
    private int _playerAdena;
    private List<L2ItemInstance> _items;

    public WarehouseWithdrawList(L2PcInstance player, int type) {
        if (player.getActiveWarehouse() == null) { return; }

        _whType = type;
        _playerAdena = player.getAdena();
        _items = player.getActiveWarehouse().getItems();
    }

    @Override
    protected final void writeImpl() {
        writeC(0x42);
        writeH(_whType);
        writeD(_playerAdena);
        writeH(_items.size());

        for (L2ItemInstance temp : _items) {
            if (temp.getItem() == null) { continue; }

            Item item = temp.getItem();
            writeH(item.getType1());
            writeD(temp.getObjectId());
            writeD(temp.getItemId());
            writeD(temp.getCount());
            writeH(item.getType2());
            writeH(temp.getCustomType1());
            writeD(item.getBodyPart().getMask());
            writeH(temp.getEnchantLevel());
            writeH(temp.getCustomType2());
            writeH(0x00);
            writeD(temp.getObjectId());
            if (temp.isAugmented()) {
                writeD(0x0000FFFF & temp.getAugmentation().getAugmentationId());
                writeD(temp.getAugmentation().getAugmentationId() >> 16);
            }
            else { writeQ(0x00); }
        }
    }
}