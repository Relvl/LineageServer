package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.List;

public class ItemList extends L2GameServerPacket {
    private final List<L2ItemInstance> _items;
    private final boolean _showWindow;

    public ItemList(L2PcInstance cha, boolean showWindow) {
        _items = cha.getInventory().getItems();
        _showWindow = showWindow;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x1b);
        writeH(_showWindow ? 0x01 : 0x00);
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
            writeH(temp.isEquipped() ? 0x01 : 0x00);
            writeD(item.getBodyPart());
            writeH(temp.getEnchantLevel());
            writeH(temp.getCustomType2());
            writeD((temp.isAugmented()) ? temp.getAugmentation().getAugmentationId() : 0x00);
            writeD(temp.getShadowMana());
        }
    }
}