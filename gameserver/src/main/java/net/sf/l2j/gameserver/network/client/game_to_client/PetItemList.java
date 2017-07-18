package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.List;

public class PetItemList extends L2GameServerPacket {
    private final List<L2ItemInstance> _items;

    public PetItemList(L2PetInstance character) {
        _items = character.getInventory().getItems();
    }

    @Override
    protected final void writeImpl() {
        writeC(0xB2);
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
            writeD(item.getBodyPart().getMask());
            writeH(temp.getEnchantLevel());
            writeH(temp.getCustomType2());
        }
    }
}