package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.item.EItemModifyState;
import net.sf.l2j.gameserver.model.item.instance.ItemInfo;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.ArrayList;
import java.util.List;

public class PetInventoryUpdate extends L2GameServerPacket {
    private final List<ItemInfo> _items;

    public PetInventoryUpdate(List<ItemInfo> items) {
        _items = items;
    }

    public PetInventoryUpdate() {
        this(new ArrayList<>());
    }

    public void addItem(L2ItemInstance item) {
        if (item != null) { _items.add(new ItemInfo(item)); }
    }

    public void addNewItem(L2ItemInstance item) {
        if (item != null) { _items.add(new ItemInfo(item, EItemModifyState.ADDED)); }
    }

    public void addModifiedItem(L2ItemInstance item) {
        if (item != null) { _items.add(new ItemInfo(item, EItemModifyState.MODIFIED)); }
    }

    public void addRemovedItem(L2ItemInstance item) {
        if (item != null) { _items.add(new ItemInfo(item, EItemModifyState.REMOVED)); }
    }

    public void addItems(List<L2ItemInstance> items) {
        if (items != null) { for (L2ItemInstance item : items) { if (item != null) { _items.add(new ItemInfo(item)); } } }
    }

    @Override
    protected final void writeImpl() {
        writeC(0xb3);
        writeH(_items.size());

        for (ItemInfo temp : _items) {
            if (temp == null || temp.getItem() == null) { continue; }

            Item item = temp.getItem();
            writeH(temp.getModifyState());
            writeH(item.getType1());
            writeD(temp.getObjectId());
            writeD(item.getItemId());
            writeD(temp.getCount());
            writeH(item.getType2());
            writeH(temp.getCustomType1());
            writeH(temp.getEquipped());
            writeD(item.getBodyPart());
            writeH(temp.getEnchant());
            writeH(temp.getCustomType2());
        }
    }
}