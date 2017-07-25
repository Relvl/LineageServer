package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.model.DropProtection;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.EMaskedItemType;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.taskmanager.ItemsOnGroundTaskManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class L2ItemInstance extends L2Object {
    private static final Logger _logItems = Logger.getLogger("item");

    private final int itemId;
    private final Item item;
    private final ReentrantLock dbLock = new ReentrantLock();
    private final DropProtection dropProtection = new DropProtection();
    private int ownerId;
    private int dropperObjectId;
    private int count;
    private long time;
    private EItemLocation itemLocation;
    /** TODO! Это и слот на хранении, и слот на кукле. Дурость, надо переделать на раздельное. */
    private int paperdollSlot;
    private int enchantLevel;
    private L2Augmentation augmentation;
    private int shadowMana = -1;
    /** Этот тип используется для передачи данных по билетам лото и забегов. */
    private int customType1;
    private int customType2;
    private boolean destroyProtected;
    private EItemModifyState itemModifyState = EItemModifyState.MODIFIED;
    private boolean existsInDb;
    private boolean storedInDb;
    private ScheduledFuture<?> itemLootTask;
    private int shotsMask;

    public L2ItemInstance(int objectId, int itemId) {
        super(objectId);
        this.itemId = itemId;
        item = ItemTable.getInstance().getTemplate(itemId);
        if (this.itemId == 0 || item == null) { throw new IllegalArgumentException(); }

        setName(item.getName());
        setCount(1);
        itemLocation = EItemLocation.VOID;
        customType1 = 0;
        customType2 = 0;
        shadowMana = item.getDuration() * 60;
    }

    public L2ItemInstance(int objectId, Item item) {
        super(objectId);
        itemId = item.getItemId();
        this.item = item;

        setName(this.item.getName());
        setCount(1);

        itemLocation = EItemLocation.VOID;
        shadowMana = this.item.getDuration() * 60;
    }

    public static L2ItemInstance restoreFromDb(int ownerId, ResultSet rs) {
        L2ItemInstance inst = null;
        int objectId;
        int item_id;
        int paperdollSlot;
        int enchant_level;
        int custom_type1;
        int custom_type2;
        int manaLeft;
        int count;
        long time;
        EItemLocation loc;
        try {
            objectId = rs.getInt(1);
            item_id = rs.getInt("item_id");
            count = rs.getInt("count");
            loc = EItemLocation.valueOf(rs.getString("loc"));
            paperdollSlot = rs.getInt("loc_data");
            enchant_level = rs.getInt("enchant_level");
            custom_type1 = rs.getInt("custom_type1");
            custom_type2 = rs.getInt("custom_type2");
            manaLeft = rs.getInt("mana_left");
            time = rs.getLong("time");
        }
        catch (Exception e) {
            LOGGER.error("Could not restore an item owned by {} from DB:", ownerId, e);
            return null;
        }

        Item item = ItemTable.getInstance().getTemplate(item_id);
        if (item == null) {
            LOGGER.error("Item item_id={} not known, object_id={}", item_id, objectId);
            return null;
        }

        inst = new L2ItemInstance(objectId, item);
        inst.ownerId = ownerId;
        inst.setCount(count);
        inst.enchantLevel = enchant_level;
        inst.customType1 = custom_type1;
        inst.customType2 = custom_type2;
        inst.itemLocation = loc;
        inst.paperdollSlot = paperdollSlot;
        inst.existsInDb = true;
        inst.storedInDb = true;

        inst.shadowMana = manaLeft;
        inst.time = time;

        if (inst.isEquipable()) { inst.restoreAttributes(); }

        return inst;
    }

    public void setOwnerId(EItemProcessPurpose process, int ownerId, L2PcInstance creator, L2Object reference) {
        setOwnerId(ownerId);
        if (Config.LOG_ITEMS) {
            LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
            record.setLoggerName("item");
            record.setParameters(new Object[]{this, creator, reference});
            _logItems.log(record);
        }
    }

    public int getOwnerId() { return ownerId; }

    public void setOwnerId(int ownerId) {
        if (ownerId == this.ownerId) { return; }
        this.ownerId = ownerId;
        storedInDb = false;
    }

    public void setLocation(EItemLocation itemLocation, int paperdollSlot) {
        if (itemLocation == this.itemLocation && paperdollSlot == this.paperdollSlot) { return; }
        this.itemLocation = itemLocation;
        storedInDb = false;
        this.paperdollSlot = paperdollSlot;
    }

    public EItemLocation getLocation() { return itemLocation; }

    @Deprecated
    public void setLocation(EItemLocation loc) { setLocation(loc, 0); }

    public int getCount() { return count; }

    public void setCount(int count) {
        if (this.count == count) { return; }
        this.count = count >= -1 ? count : 0;
        storedInDb = false;
    }

    public void changeCount(EItemProcessPurpose process, int count, L2PcInstance creator, L2Object reference) {
        if (count == 0) { return; }
        if (count > 0 && this.count > Integer.MAX_VALUE - count) { setCount(Integer.MAX_VALUE); }
        else { setCount(this.count + count); }
        if (this.count < 0) { setCount(0); }

        storedInDb = false;

        if (Config.LOG_ITEMS && process != null) {
            LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
            record.setLoggerName("item");
            record.setParameters(new Object[]{this, creator, reference});
            _logItems.log(record);
        }
    }

    public void changeCountWithoutTrace(int count, L2PcInstance creator, L2Object reference) { changeCount(null, count, creator, reference); }

    public boolean isEquipable() { return !(item.getBodyPart() == EItemBodyPart.SLOT_NONE || item.getItemType() == EtcItemType.ARROW || item.getItemType() == EtcItemType.LURE); }

    public boolean isEquipped() { return itemLocation == EItemLocation.PAPERDOLL || itemLocation == EItemLocation.PET_EQUIP; }

    public int getLocationSlot() {
        assert itemLocation == EItemLocation.PAPERDOLL || itemLocation == EItemLocation.PET_EQUIP || itemLocation == EItemLocation.FREIGHT;
        return paperdollSlot;
    }

    public Item getItem() { return item; }

    public int getCustomType1() { return customType1; }

    public void setCustomType1(int newtype) { customType1 = newtype; }

    public int getCustomType2() { return customType2; }

    public void setCustomType2(int newtype) { customType2 = newtype; }

    public EMaskedItemType getItemType() { return item.getItemType(); }

    public int getItemId() { return itemId; }

    public boolean isEtcItem() { return item instanceof EtcItem; }

    public boolean isWeapon() { return item instanceof Weapon; }

    public boolean isArmor() { return item instanceof Armor; }

    public EtcItem getEtcItem() { return isEtcItem() ? (EtcItem) item : null; }

    public int getCrystalCount() { return item.getCrystalCount(enchantLevel); }

    public int getReferencePrice() { return item.getReferencePrice(); }

    public EItemModifyState getModifyState() { return itemModifyState; }

    public void setModifyState(EItemModifyState lastChange) { itemModifyState = lastChange; }

    public boolean isStackable() { return item.isStackable(); }

    public boolean isDropable() { return !isAugmented() && item.isDropable(); }

    public boolean isDestroyable() { return !isQuestItem() && item.isDestroyable(); }

    public boolean isTradable() { return !isAugmented() && item.isTradable(); }

    public boolean isSellable() { return !isAugmented() && item.isSellable(); }

    public boolean isDepositable(boolean isPrivateWareHouse) {
        if (isEquipped() || !item.isDepositable()) { return false; }
        if (!isPrivateWareHouse) {
            if (!isTradable() || isShadowItem()) {
                return false;
            }
        }
        return true;
    }

    public boolean isAvailable(L2PcInstance player, boolean allowAdena, boolean allowNonTradable) {
        return !isEquipped() // Not equipped
                && (item.getType2() != EItemType2.TYPE2_QUEST) // Not Quest Item
                && (item.getType2() != EItemType2.TYPE2_MONEY || item.getType1() != EItemType1.SHIELD_ARMOR) // not money, not shield
                && (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item of currently summoned pet
                && (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
                && (allowAdena || itemId != ItemConst.ADENA_ID) // Not adena
                && (player.getCurrentSkill().getSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != itemId)
                && (!player.isCastingSimultaneouslyNow() || player.getLastSimultaneousSkillCast() == null || player.getLastSimultaneousSkillCast().getItemConsumeId() != itemId)
                && (allowNonTradable || isTradable());
    }

    @Override
    public void onAction(L2PcInstance player) {
        if (item.getItemType() == EtcItemType.CASTLE_GUARD) {
            if (player.isInParty()) {
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
            int castleId = MercTicketManager.getTicketCastleId(itemId);
            if (castleId > 0 && !player.isCastleLord(castleId)) {
                player.sendPacket(SystemMessageId.THIS_IS_NOT_A_MERCENARY_OF_A_CASTLE_THAT_YOU_OWN_AND_SO_CANNOT_CANCEL_POSITIONING);
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
        }
        if (player.isFlying()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        player.getAI().setIntention(EIntention.PICK_UP, this);
    }

    @Override
    public void onActionShift(L2PcInstance player) {
        if (player.isGM()) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile("data/html/admin/iteminfo.htm");
            html.replace("%objid%", getObjectId());
            html.replace("%itemid%", itemId);
            html.replace("%ownerid%", ownerId);
            html.replace("%loc%", itemLocation.toString());
            html.replace("%class%", getClass().getSimpleName());
            player.sendPacket(html);
        }
        super.onActionShift(player);
    }

    public int getEnchantLevel() { return enchantLevel; }

    public void setEnchantLevel(int enchantLevel) {
        if (this.enchantLevel == enchantLevel) { return; }
        this.enchantLevel = enchantLevel;
        storedInDb = false;
    }

    public boolean isAugmented() { return augmentation != null; }

    public L2Augmentation getAugmentation() { return augmentation; }

    public boolean setAugmentation(L2Augmentation augmentation) {
        if (this.augmentation == null) {
            this.augmentation = augmentation;
            updateItemAttributes(null);
            return true;
        }
        return false;
    }

    public void removeAugmentation() {
        if (augmentation == null) { return; }
        augmentation = null;
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id = ?");
            statement.setInt(1, getObjectId());
            statement.executeUpdate();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Could not remove augmentation for item: {} from DB: ", this, e);
        }
    }

    private void restoreAttributes() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT attributes,skill_id,skill_level FROM augmentations WHERE item_id=?");
            statement.setInt(1, getObjectId());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int aug_attributes = rs.getInt(1);
                int aug_skillId = rs.getInt(2);
                int aug_skillLevel = rs.getInt(3);
                if (aug_attributes != -1 && aug_skillId != -1 && aug_skillLevel != -1) { augmentation = new L2Augmentation(rs.getInt("attributes"), rs.getInt("skill_id"), rs.getInt("skill_level")); }
            }
            rs.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Could not restore augmentation data for item {} from DB: {}", this, e.getMessage(), e);
        }
    }

    private void updateItemAttributes(Connection pooledCon) {
        try (Connection con = pooledCon == null ? L2DatabaseFactoryOld.getInstance().getConnection() : pooledCon) {
            PreparedStatement statement = con.prepareStatement("REPLACE INTO augmentations VALUES(?,?,?,?)");
            statement.setInt(1, getObjectId());
            if (augmentation == null) {
                statement.setInt(2, -1);
                statement.setInt(3, -1);
                statement.setInt(4, -1);
            }
            else {
                statement.setInt(2, augmentation.getAttributes());
                if (augmentation.getSkill() == null) {
                    statement.setInt(3, 0);
                    statement.setInt(4, 0);
                }
                else {
                    statement.setInt(3, augmentation.getSkill().getId());
                    statement.setInt(4, augmentation.getSkill().getLevel());
                }
            }
            statement.executeUpdate();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Could not update attributes for item: {} from DB: ", this, e);
        }
    }

    public boolean isShadowItem() { return shadowMana >= 0; }

    public int decreaseMana(int period) {
        storedInDb = false;
        return shadowMana -= period;
    }

    public int getShadowMana() { return shadowMana / 60; }

    @Override
    public boolean isAutoAttackable(L2Character attacker) { return false; }

    public List<Func> getStatFuncs(L2Character player) { return item.getStatFuncs(this, player); }

    public void updateDatabase() {
        dbLock.lock();

        try {
            if (existsInDb) {
                if (ownerId == 0 || itemLocation == EItemLocation.VOID || (count == 0 && itemLocation != EItemLocation.LEASE)) { removeFromDb(); }
                else { updateInDb(); }
            }
            else {
                if (ownerId == 0 || itemLocation == EItemLocation.VOID || (count == 0 && itemLocation != EItemLocation.LEASE)) { return; }
                insertIntoDb();
            }
        }
        finally {
            dbLock.unlock();
        }
    }

    public void dropMe(L2Character dropper, int x, int y, int z) {
        ThreadPoolManager.getInstance().executeTask(new ItemDropTask(this, dropper, x, y, z));
    }

    public void pickupMe(L2Character player) {
        assert getPosition().getWorldRegion() != null;

        L2WorldRegion oldregion = getPosition().getWorldRegion();
        player.broadcastPacket(new GetItem(this, player.getObjectId()));

        synchronized (this) {
            setIsVisible(false);
            getPosition().setWorldRegion(null);
        }

        if (MercTicketManager.getTicketCastleId(itemId) > 0) { MercTicketManager.getInstance().removeTicket(this); }

        if (itemId == ItemConst.ADENA_ID || itemId == 6353) {
            L2PcInstance actor = player.getActingPlayer();
            if (actor != null) {
                QuestState qs = actor.getQuestState("Tutorial");
                if (qs != null) { qs.getQuest().notifyEvent("CE" + itemId + "", null, actor); }
            }
        }

        L2World.getInstance().removeVisibleObject(this, oldregion);
    }

    private void updateInDb() {
        assert existsInDb;

        if (storedInDb) { return; }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=?,time=? WHERE object_id = ?");
            statement.setInt(1, ownerId);
            statement.setInt(2, count);
            statement.setString(3, itemLocation.name());
            statement.setInt(4, paperdollSlot);
            statement.setInt(5, enchantLevel);
            statement.setInt(6, customType1);
            statement.setInt(7, customType2);
            statement.setInt(8, shadowMana);
            statement.setLong(9, time);
            statement.setInt(10, getObjectId());
            statement.executeUpdate();
            existsInDb = true;
            storedInDb = true;
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not update item {} in DB: Reason: {}", this, e.getMessage(), e);
        }
    }

    private void insertIntoDb() {
        assert !existsInDb && getObjectId() != 0;

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left,time) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
            statement.setInt(1, ownerId);
            statement.setInt(2, itemId);
            statement.setInt(3, count);
            statement.setString(4, itemLocation.name());
            statement.setInt(5, paperdollSlot);
            statement.setInt(6, enchantLevel);
            statement.setInt(7, getObjectId());
            statement.setInt(8, customType1);
            statement.setInt(9, customType2);
            statement.setInt(10, shadowMana);
            statement.setLong(11, time);

            statement.executeUpdate();
            existsInDb = true;
            storedInDb = true;
            statement.close();

            if (augmentation != null) { updateItemAttributes(con); }
        }
        catch (Exception e) {
            LOGGER.error("Could not insert item {} into DB: Reason: {}", this, e.getMessage(), e);
        }
    }

    private void removeFromDb() {
        assert existsInDb;

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE object_id=?");
            statement.setInt(1, getObjectId());
            statement.executeUpdate();
            existsInDb = false;
            storedInDb = false;
            statement.close();

            statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id = ?");
            statement.setInt(1, getObjectId());
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not delete item {} in DB: {}", this, e.getMessage(), e);
        }
    }

    @Override
    public String toString() { return item.toString(); }

    public void resetOwnerTimer() {
        if (itemLootTask != null) { itemLootTask.cancel(true); }
        itemLootTask = null;
    }

    public ScheduledFuture<?> getItemLootTask() { return itemLootTask; }

    public void setItemLootTask(ScheduledFuture<?> sf) { itemLootTask = sf; }

    public boolean isDestroyProtected() { return destroyProtected; }

    public void setDestroyProtected(boolean destroyProtected) { this.destroyProtected = destroyProtected; }

    public boolean isNightLure() { return (itemId >= 8505 && itemId <= 8513) || itemId == 8485; }

    public long getTime() { return time; }

    public void setTime(int time) { this.time = time > 0 ? time : 0; }

    public boolean isPetItem() { return item.isPetItem(); }

    public boolean isPotion() { return item.isPotion(); }

    public boolean isElixir() { return item.isElixir(); }

    public boolean isHerb() { return item.getItemType() == EtcItemType.HERB; }

    public boolean isHeroItem() { return item.isHeroItem(); }

    public boolean isQuestItem() { return item.isQuestItem(); }

    @Override
    public void decayMe() {
        ItemsOnGroundTaskManager.getInstance().remove(this);
        super.decayMe();
    }

    public void setDropperObjectId(int id) { dropperObjectId = id; }

    public DropProtection getDropProtection() { return dropProtection; }

    @Override
    public void sendInfo(L2PcInstance activeChar) {
        activeChar.sendPacket(dropperObjectId == 0 ? new SpawnItem(this) : new DropItem(this, dropperObjectId));
    }

    public List<Quest> getQuestEvents() { return item.getQuestEvents(); }

    @Override
    public boolean isChargedShot(ShotType type) {
        return (shotsMask & type.getMask()) == type.getMask();
    }

    @Override
    public void setChargedShot(ShotType type, boolean charged) {
        if (charged) { shotsMask |= type.getMask(); }
        else { shotsMask &= ~type.getMask(); }
    }

    public void unChargeAllShots() { shotsMask = 0; }
}