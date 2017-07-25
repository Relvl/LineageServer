package net.sf.l2j.gameserver.network.client.game_to_client.gm;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.client.game_to_client.L2GameServerPacket;

import java.util.List;

public class GMViewItemList extends L2GameServerPacket {
    private final List<L2ItemInstance> _items;
    private final int _limit;
    private final String _playerName;

    public GMViewItemList(L2PcInstance cha) {
        _items = cha.getInventory().getItems();
        _playerName = cha.getName();
        _limit = cha.getInventoryLimit();
    }

    public GMViewItemList(L2PetInstance cha) {
        _items = cha.getInventory().getItems();
        _playerName = cha.getName();
        _limit = Config.INVENTORY_MAXIMUM_PET;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x94);
        writeS(_playerName);
        writeD(_limit);
        writeH(0x01); // show window ??
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