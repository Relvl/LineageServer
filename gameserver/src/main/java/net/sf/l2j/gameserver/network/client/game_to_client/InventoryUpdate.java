package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.item.EItemModifyState;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class InventoryUpdate extends L2GameServerPacket {
    private final Map<L2ItemInstance, EItemModifyState> itemMap = new HashMap<>();

    public void addItem(L2ItemInstance item) {
        if (item != null) {
            itemMap.put(item, item.getModifyState());
        }
    }

    public void addNewItem(L2ItemInstance item) {
        if (item != null) {
            itemMap.put(item, EItemModifyState.ADDED);
        }
    }

    public void addModifiedItem(L2ItemInstance item) {
        if (item != null) {
            itemMap.put(item, EItemModifyState.MODIFIED);
        }
    }

    public void addRemovedItem(L2ItemInstance item) {
        if (item != null) {
            itemMap.put(item, EItemModifyState.REMOVED);
        }
    }

    public void addItems(Iterable<L2ItemInstance> items) {
        if (items != null) {
            for (L2ItemInstance item : items) {
                if (item != null) {
                    itemMap.put(item, item.getModifyState());
                }
            }
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(getPacketId());
        writeH(itemMap.size());

        for (Entry<L2ItemInstance, EItemModifyState> entry : itemMap.entrySet()) {
            if (entry.getKey() == null || entry.getKey().getItem() == null) {continue;}
            Item item = entry.getKey().getItem();

            writeH(entry.getValue());
            writeH(item.getType1());
            writeD(entry.getKey().getObjectId());
            writeD(item.getItemId());
            writeD(entry.getKey().getCount());
            writeH(item.getType2());
            writeH(entry.getKey().getCustomType1());
            writeH(entry.getKey().isEquipped());
            writeD(item.getBodyPart());
            writeH(entry.getKey().getEnchantLevel());
            writeH(entry.getKey().getCustomType2());
            if (isPlayerPacket()) {
                writeD(entry.getKey().getAugmentation() == null ? 0 : entry.getKey().getAugmentation().getAugmentationId());
                writeD(entry.getKey().getShadowMana());
            }
        }
    }

    protected int getPacketId() { return 0x27; }

    protected boolean isPlayerPacket() { return true; }

    @Override
    public String toString() {
        return "InventoryUpdate{" + itemMap + '}';
    }
}