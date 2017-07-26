package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.*;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.world.L2World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@SuppressWarnings("ObjectEquality")
public abstract class ItemContainer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ItemContainer.class);

    protected final List<L2ItemInstance> items = new CopyOnWriteArrayList<>();

    protected ItemContainer() {}

    protected abstract L2Character getOwner();

    protected abstract EItemLocation getBaseLocation();

    public String getName() { return "ItemContainer"; }

    public EItemProcessPurpose getItemInteractionPurpose() { return EItemProcessPurpose.ITEM_CONTAINER; }

    public int getOwnerId() {
        return (getOwner() == null) ? 0 : getOwner().getObjectId();
    }

    public int getSize() { return items.size(); }

    public List<L2ItemInstance> getItems() { return items; }

    @Deprecated
    public boolean hasAtLeastOneItem(int... itemIds) {
        for (int itemId : itemIds) {
            if (getItemByItemId(itemId) != null) { return true; }
        }
        return false;
    }

    public List<L2ItemInstance> getItemsByItemId(int itemId) {
        return items.stream()
                .filter(item -> item.getItemId() == itemId)
                .collect(Collectors.toList());
    }

    public L2ItemInstance getItemByItemId(int itemId) {
        return items.stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst()
                .orElse(null);
    }

    public L2ItemInstance getItemByObjectId(int objectId) {
        return items.stream()
                .filter(item -> item.getObjectId() == objectId)
                .findFirst()
                .orElse(null);
    }

    public int getInventoryItemCount(int itemId, int enchantLevel) {
        int count = 0;
        for (L2ItemInstance item : items) {
            if (item.getItemId() == itemId && (item.getEnchantLevel() == enchantLevel || enchantLevel < 0)) {
                if (item.isStackable()) {
                    return item.getCount();
                }
                count++;
            }
        }
        return count;
    }

    public L2ItemInstance addItem(EItemProcessPurpose process, L2ItemInstance item, L2PcInstance actor, L2Object reference) {
        L2ItemInstance olditem = getItemByItemId(item.getItemId());

        if (olditem != null && olditem.isStackable()) {
            int count = item.getCount();
            olditem.changeCount(process, count, actor, reference);
            olditem.setModifyState(EItemModifyState.MODIFIED);

            ItemTable.getInstance().destroyItem(process, item, actor, reference);
            item.updateDatabase();
            item = olditem;

            if (item.getItemId() == ItemConst.ADENA_ID && count < 10000 * Config.RATE_DROP_ADENA) {
                // Не только лишь вся адена сохраняется сразу... Мало какая это делает.
                if (Rnd.get(10) < 2) {
                    item.updateDatabase();
                }
            }
            else {
                item.updateDatabase();
            }
        }
        else {
            item.setOwnerId(process, getOwnerId(), actor, reference);
            item.setLocation(getBaseLocation());
            item.setModifyState(EItemModifyState.ADDED);
            addItem(item);
            item.updateDatabase();
        }

        refreshWeight();
        return item;
    }

    public L2ItemInstance addItem(EItemProcessPurpose process, int itemId, int count, L2PcInstance actor, L2Object reference) {
        L2ItemInstance item = getItemByItemId(itemId);

        if (item != null && item.isStackable()) {
            item.changeCount(process, count, actor, reference);
            item.setModifyState(EItemModifyState.MODIFIED);

            if (itemId == ItemConst.ADENA_ID && count < 10000 * Config.RATE_DROP_ADENA) {
                if (Rnd.get(10) < 2) {
                    item.updateDatabase();
                }
            }
            else {
                item.updateDatabase();
            }
        }
        else {
            for (int i = 0; i < count; i++) {
                Item template = ItemTable.getInstance().getTemplate(itemId);
                if (template == null) {
                    LOGGER.warn("[{}] Invalid ItemId requested: {}", actor != null ? actor.getName() : "", itemId);
                    return null;
                }

                item = ItemTable.getInstance().createItem(process, itemId, template.isStackable() ? count : 1, actor, reference);
                item.setOwnerId(getOwnerId());
                item.setLocation(getBaseLocation());
                item.setModifyState(EItemModifyState.ADDED);

                addItem(item);
                item.updateDatabase();
                if (template.isStackable() || !Config.MULTIPLE_ITEM_DROP) { break; }
            }
        }

        refreshWeight();
        return item;
    }

    public L2ItemInstance transferItem(EItemProcessPurpose process, int objectId, int count, ItemContainer target, L2PcInstance actor, L2Object reference) {
        if (target == null) { return null; }

        L2ItemInstance sourceitem = getItemByObjectId(objectId);
        if (sourceitem == null) { return null; }

        L2ItemInstance targetitem = sourceitem.isStackable() ? target.getItemByItemId(sourceitem.getItemId()) : null;

        synchronized (sourceitem) {
            if (getItemByObjectId(objectId) != sourceitem) { return null; }

            if (count > sourceitem.getCount()) {
                count = sourceitem.getCount();
            }

            if (sourceitem.getCount() == count && targetitem == null) {
                removeItem(sourceitem);
                target.addItem(process, sourceitem, actor, reference);
                targetitem = sourceitem;
            }
            else {
                if (sourceitem.getCount() > count) {
                    sourceitem.changeCount(process, -count, actor, reference);
                }
                else {
                    removeItem(sourceitem);
                    ItemTable.getInstance().destroyItem(process, sourceitem, actor, reference);
                }

                if (targetitem != null) {
                    targetitem.changeCount(process, count, actor, reference);
                }
                else {
                    targetitem = target.addItem(process, sourceitem.getItemId(), count, actor, reference);
                }
            }
            sourceitem.updateDatabase();
            if (targetitem != sourceitem && targetitem != null) {
                targetitem.updateDatabase();
            }
            if (sourceitem.isAugmented()) {
                sourceitem.getAugmentation().removeBonus(actor);
            }
            refreshWeight();
            target.refreshWeight();
        }
        return targetitem;
    }

    // -===========================================================================================================-

    public L2ItemInstance destroyItemByItemId(EItemProcessPurpose process, int itemId, int count, L2PcInstance actor, L2Object reference, boolean sendMessage) {
        return destroyItem(process, getItemByItemId(itemId), count, actor, reference, sendMessage);
    }

    public L2ItemInstance destroyItem(EItemProcessPurpose process, int objectId, int count, L2PcInstance actor, L2Object reference, boolean sendMessage) {
        return destroyItem(process, getItemByObjectId(objectId), count, actor, reference, sendMessage);
    }

    protected L2ItemInstance destroyItem(EItemProcessPurpose process, L2ItemInstance item, int count, L2PcInstance actor, L2Object reference, boolean sendMessage) {
        if (item == null) { return null; }
        synchronized (item) {
            if (item.getCount() > count) {
                item.changeCount(process, -count, actor, reference);
                item.setModifyState(EItemModifyState.MODIFIED);
                if (process != null || Rnd.get(10) == 0) {
                    item.updateDatabase();
                }
                refreshWeight();
                return item;
            }

            if (item.getCount() < count) { return null; }

            boolean removed = removeItem(item);
            if (!removed) { return null; }

            ItemTable.getInstance().destroyItem(process, item, actor, reference);
            item.updateDatabase();
            refreshWeight();
        }
        return item;
    }

    public void destroyAllItems(EItemProcessPurpose process, L2PcInstance actor) {
        for (L2ItemInstance item : items) {
            destroyItem(process, item, item.getCount(), actor, null, false);
        }
    }
    // -===========================================================================================================-

    public int getAdena() {
        for (L2ItemInstance item : items) {
            if (item.getItemId() == ItemConst.ADENA_ID) { return item.getCount(); }
        }
        return 0;
    }

    protected void addItem(L2ItemInstance item) { items.add(item); }

    protected boolean removeItem(L2ItemInstance item) { return items.remove(item); }

    protected void refreshWeight() { }

    public void deleteMe() {
        if (getOwner() != null) {
            for (L2ItemInstance item : items) {
                item.updateDatabase();
                L2World.getInstance().removeObject(item);
            }
        }
        items.clear();
    }

    public void restore() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=?)");
            statement.setInt(1, getOwnerId());
            statement.setString(2, getBaseLocation().name());
            ResultSet inv = statement.executeQuery();

            while (inv.next()) {
                L2ItemInstance item = L2ItemInstance.restoreFromDb(getOwnerId(), inv);
                if (item == null) { continue; }

                L2World.getInstance().addObject(item);

                L2PcInstance owner = (getOwner() == null) ? null : getOwner().getActingPlayer();

                // If stackable item is found in inventory just add to current quantity
                if (item.isStackable() && getItemByItemId(item.getItemId()) != null) { addItem(EItemProcessPurpose.RESTORE, item, owner, null); }
                else { addItem(item); }
            }
            inv.close();
            statement.close();
            refreshWeight();
        }
        catch (Exception e) {
            LOGGER.error("could not restore container:", e);
        }
    }

    public boolean validateCapacity(int slots) {
        return true;
    }

    public boolean validateWeight(int weight) {
        return true;
    }
}