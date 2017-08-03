package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemModifyState;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.skills.DocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class ItemTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemTable.class);

    private Item[] templates;
    private static final Map<Integer, Armor> ARMORS = new HashMap<>();
    private static final Map<Integer, EtcItem> ETC_ITEMS = new HashMap<>();
    private static final Map<Integer, Weapon> WEAPONS = new HashMap<>();

    public static ItemTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    protected ItemTable() {
        load();
    }

    private void load() {
        File dir = new File("./data/xml/items");

        int highest = 0;
        for (File file : dir.listFiles()) {
            DocumentItem document = new DocumentItem(file);
            document.parse();

            for (Item item : document.getItemList()) {
                if (highest < item.getItemId()) { highest = item.getItemId(); }
                if (item instanceof EtcItem) { ETC_ITEMS.put(item.getItemId(), (EtcItem) item); }
                else if (item instanceof Armor) { ARMORS.put(item.getItemId(), (Armor) item); }
                else { WEAPONS.put(item.getItemId(), (Weapon) item); }
            }
        }

        templates = new Item[highest + 1];

        for (Armor item : ARMORS.values()) { templates[item.getItemId()] = item; }
        for (Weapon item : WEAPONS.values()) { templates[item.getItemId()] = item; }
        for (EtcItem item : ETC_ITEMS.values()) { templates[item.getItemId()] = item; }
    }

    public Item getTemplate(int id) {
        if (id >= templates.length) { return null; }
        return templates[id];
    }

    public L2ItemInstance createItem(EItemProcessPurpose process, int itemId, int count, L2PcInstance actor, L2Object reference) {
        L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);

        if (process == EItemProcessPurpose.LOOT) {
            ScheduledFuture<?> itemLootShedule;
            if (reference instanceof L2Attackable && ((L2Character) reference).isRaid()) {
                L2Attackable raidBoss = (L2Attackable) reference;
                if (raidBoss.getFirstCommandChannelAttacked() != null) {
                    item.setOwnerId(raidBoss.getFirstCommandChannelAttacked().getChannelLeader().getObjectId());
                    itemLootShedule = ThreadPoolManager.getInstance().scheduleGeneral(new ResetOwner(item), 300000);
                    item.setItemLootTask(itemLootShedule);
                }
            }
            else if (!Config.AUTO_LOOT) {
                item.setOwnerId(actor.getObjectId());
                itemLootShedule = ThreadPoolManager.getInstance().scheduleGeneral(new ResetOwner(item), 15000);
                item.setItemLootTask(itemLootShedule);
            }
        }

        L2World.getInstance().addObject(item);

        if (item.isStackable() && count > 1) { item.setCount(count); }

        if (Config.LOG_ITEMS) {
            LOGGER.info("ITEM CREATE: {}, {}, {}, {}", process, item, actor, reference);
        }

        return item;
    }

    public L2ItemInstance createDummyItem(int itemId) {
        Item item = getTemplate(itemId);
        if (item == null) { return null; }
        return new L2ItemInstance(0, item);
    }

    public void destroyItem(EItemProcessPurpose process, L2ItemInstance item, L2PcInstance actor, L2Object reference) {
        synchronized (item) {
            item.setCount(0);
            item.setOwnerId(0);
            item.setLocation(EItemLocation.VOID);
            item.setModifyState(EItemModifyState.REMOVED);

            L2World.getInstance().removeObject(item);
            IdFactory.getInstance().releaseId(item.getObjectId());

            if (Config.LOG_ITEMS) {
                LOGGER.info("ITEM DELETE: {}, {}, {}, {}", process, item, actor, reference);
            }

            if (PetDataTable.isPetCollar(item.getItemId())) {
                try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
                    PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
                    statement.setInt(1, item.getObjectId());
                    statement.execute();
                    statement.close();
                }
                catch (SQLException e) {
                    LOGGER.error("could not delete pet objectid:", e);
                }
            }
        }
    }

    public void reload() {
        ARMORS.clear();
        ETC_ITEMS.clear();
        WEAPONS.clear();
        load();
    }

    protected static class ResetOwner implements Runnable {
        private final L2ItemInstance itemInstance;

        public ResetOwner(L2ItemInstance itemInstance) {
            this.itemInstance = itemInstance;
        }

        @Override
        public void run() {
            itemInstance.setOwnerId(0);
            itemInstance.setItemLootTask(null);
        }
    }

    private static final class SingletonHolder {
        private static final ItemTable INSTANCE = new ItemTable();
    }
}