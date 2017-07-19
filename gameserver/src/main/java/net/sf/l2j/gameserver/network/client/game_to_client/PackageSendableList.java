package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

public class PackageSendableList extends L2GameServerPacket {
    private final L2ItemInstance[] _items;
    private final int _playerObjId;

    public PackageSendableList(L2ItemInstance[] items, int playerObjId) {
        _items = items;
        _playerObjId = playerObjId;
    }

    @Override
    protected void writeImpl() {
        writeC(0xC3);
        writeD(_playerObjId);
        writeD(getClient().getActiveChar().getAdena());
        writeD(_items.length);

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
        }
    }
}