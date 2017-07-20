package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.geoengine.PathFinding;
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
import net.sf.l2j.gameserver.model.location.Location;
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

/**
 * This class manages items.
 */
public final class L2ItemInstance extends L2Object {
    private static final Logger _logItems = Logger.getLogger("item");

    private final int itemId;
    private final Item item;
    private final ReentrantLock _dbLock = new ReentrantLock();
    private final DropProtection _dropProtection = new DropProtection();
    private int _ownerId;
    private int _dropperObjectId;
    private int _count;
    private int _initCount;
    private long _time;
    private boolean _decrease;
    /** Location of the item : Inventory, PaperDoll, WareHouse */
    private EItemLocation _loc;
    /** TODO! Это и слот на хранении, и слот на кукле. Дурость, надо переделать на раздельное. */
    private int paperdollSlot;
    private int _enchantLevel;
    private L2Augmentation _augmentation;
    /** Shadow item */
    private int _mana = -1;
    /** Custom item types (used loto, race tickets) */
    private int _type1;
    private int _type2;
    private boolean _destroyProtected;
    private EItemModifyState itemModifyState = EItemModifyState.MODIFIED; // 1 added, 2 modified, 3 removed
    private boolean _existsInDb; // if a record exists in DB.
    private boolean _storedInDb; // if DB data is up-to-date.
    private ScheduledFuture<?> _itemLootShedule;
    private int _shotsMask;

    public L2ItemInstance(int objectId, int itemId) {
        super(objectId);
        this.itemId = itemId;
        item = ItemTable.getInstance().getTemplate(itemId);

        if (this.itemId == 0 || item == null) { throw new IllegalArgumentException(); }

        setName(item.getName());
        setCount(1);
        _loc = EItemLocation.VOID;
        _type1 = 0;
        _type2 = 0;
        _mana = item.getDuration() * 60;
    }

    /**
     * Constructor of the L2ItemInstance from the objetId and the description of the item given by the L2Item.
     *
     * @param objectId : int designating the ID of the object in the world
     * @param item     : L2Item containing informations of the item
     */
    public L2ItemInstance(int objectId, Item item) {
        super(objectId);
        itemId = item.getItemId();
        this.item = item;

        setName(this.item.getName());
        setCount(1);

        _loc = EItemLocation.VOID;
        _mana = this.item.getDuration() * 60;
    }

    /**
     * @param ownerId : objectID of the owner.
     * @param rs      : the ResultSet of the item.
     * @return a L2ItemInstance stored in database from its objectID
     */
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
        inst._ownerId = ownerId;
        inst.setCount(count);
        inst._enchantLevel = enchant_level;
        inst._type1 = custom_type1;
        inst._type2 = custom_type2;
        inst._loc = loc;
        inst.paperdollSlot = paperdollSlot;
        inst._existsInDb = true;
        inst._storedInDb = true;

        // Setup life time for shadow weapons
        inst._mana = manaLeft;
        inst._time = time;

        // load augmentation
        if (inst.isEquipable()) { inst.restoreAttributes(); }

        return inst;
    }

    /**
     * Sets the ownerID of the item
     *
     * @param process   : String Identifier of process triggering this action
     * @param owner_id  : int designating the ID of the owner
     * @param creator   : L2PcInstance Player requesting the item creation
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     */
    public void setOwnerId(EItemProcessPurpose process, int owner_id, L2PcInstance creator, L2Object reference) {
        setOwnerId(owner_id);

        if (Config.LOG_ITEMS) {
            LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
            record.setLoggerName("item");
            record.setParameters(new Object[]
                    {
                            this,
                            creator,
                            reference
                    });
            _logItems.log(record);
        }
    }

    /**
     * Returns the ownerID of the item
     *
     * @return int : ownerID of the item
     */
    public int getOwnerId() {
        return _ownerId;
    }

    /**
     * Sets the ownerID of the item
     *
     * @param owner_id : int designating the ID of the owner
     */
    public void setOwnerId(int owner_id) {
        if (owner_id == _ownerId) { return; }

        _ownerId = owner_id;
        _storedInDb = false;
    }

    public void setLocation(EItemLocation loc, int paperdollSlot) {
        if (loc == _loc && paperdollSlot == this.paperdollSlot) { return; }
        _loc = loc;
        this.paperdollSlot = paperdollSlot;
        _storedInDb = false;
    }

    public EItemLocation getLocation() {
        return _loc;
    }

    @Deprecated
    public void setLocation(EItemLocation loc) {
        setLocation(loc, 0);
    }

    /**
     * Returns the quantity of item
     *
     * @return int
     */
    public int getCount() {
        return _count;
    }

    /**
     * Sets the quantity of the item.<BR>
     * <BR>
     *
     * @param count the new count to set
     */
    public void setCount(int count) {
        if (_count == count) { return; }

        _count = count >= -1 ? count : 0;
        _storedInDb = false;
    }

    /**
     * Sets the quantity of the item.<BR>
     * <BR>
     * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
     *
     * @param process   : String Identifier of process triggering this action
     * @param count     : int
     * @param creator   : L2PcInstance Player requesting the item creation
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     */
    public void changeCount(EItemProcessPurpose process, int count, L2PcInstance creator, L2Object reference) {
        if (count == 0) { return; }

        if (count > 0 && _count > Integer.MAX_VALUE - count) { setCount(Integer.MAX_VALUE); }
        else {
            setCount(_count + count);
        }

        if (_count < 0) { setCount(0); }

        _storedInDb = false;

        if (Config.LOG_ITEMS && process != null) {
            LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
            record.setLoggerName("item");
            record.setParameters(new Object[]
                    {
                            this,
                            creator,
                            reference
                    });
            _logItems.log(record);
        }
    }

    // No logging (function designed for shots only)
    public void changeCountWithoutTrace(int count, L2PcInstance creator, L2Object reference) {
        changeCount(null, count, creator, reference);
    }

    /**
     * Returns if item is equipable
     *
     * @return boolean
     */
    public boolean isEquipable() {
        return !(item.getBodyPart() == EItemBodyPart.SLOT_NONE || item.getItemType() == EtcItemType.ARROW || item.getItemType() == EtcItemType.LURE);
    }

    /**
     * Returns if item is equipped
     *
     * @return boolean
     */
    public boolean isEquipped() {
        return _loc == EItemLocation.PAPERDOLL || _loc == EItemLocation.PET_EQUIP;
    }

    /**
     * Returns the slot where the item is stored
     *
     * @return int
     */
    public int getLocationSlot() {
        assert _loc == EItemLocation.PAPERDOLL || _loc == EItemLocation.PET_EQUIP || _loc == EItemLocation.FREIGHT;
        return paperdollSlot;
    }

    /**
     * Returns the characteristics of the item
     *
     * @return L2Item
     */
    public Item getItem() {
        return item;
    }

    public int getCustomType1() {
        return _type1;
    }

    public void setCustomType1(int newtype) {
        _type1 = newtype;
    }

    public int getCustomType2() {
        return _type2;
    }

    public void setCustomType2(int newtype) {
        _type2 = newtype;
    }

    public boolean isOlyRestrictedItem() {
        return item.isOlyRestrictedItem();
    }

    /**
     * Returns the type of item
     *
     * @return Enum
     */
    public EMaskedItemType getItemType() {
        return item.getItemType();
    }

    /**
     * Returns the ID of the item
     *
     * @return int
     */
    public int getItemId() {
        return itemId;
    }

    /**
     * Returns true if item is an EtcItem
     *
     * @return boolean
     */
    public boolean isEtcItem() {
        return item instanceof EtcItem;
    }

    /**
     * Returns true if item is a Weapon/Shield
     *
     * @return boolean
     */
    public boolean isWeapon() {
        return item instanceof Weapon;
    }

    /**
     * Returns true if item is an Armor
     *
     * @return boolean
     */
    public boolean isArmor() {
        return item instanceof Armor;
    }

    /**
     * Returns the characteristics of the L2EtcItem
     *
     * @return EtcItem
     */
    public EtcItem getEtcItem() {
        if (item instanceof EtcItem) { return (EtcItem) item; }

        return null;
    }

    /**
     * Returns the characteristics of the Weapon
     *
     * @return Weapon
     */
    public Weapon getWeaponItem() {
        if (item instanceof Weapon) { return (Weapon) item; }

        return null;
    }

    /**
     * Returns the characteristics of the L2Armor
     *
     * @return Armor
     */
    public Armor getArmorItem() {
        if (item instanceof Armor) { return (Armor) item; }

        return null;
    }

    /**
     * Returns the quantity of crystals for crystallization
     *
     * @return int
     */
    public final int getCrystalCount() {
        return item.getCrystalCount(_enchantLevel);
    }

    /**
     * @return the reference price of the item.
     */
    public int getReferencePrice() {
        return item.getReferencePrice();
    }

    /**
     * @return the name of the item.
     */
    public String getItemName() {
        return item.getName();
    }

    /**
     * @return the last change of the item.
     */
    public EItemModifyState getModifyState() {
        return itemModifyState;
    }

    /**
     * Sets the last change of the item
     *
     * @param lastChange : int
     */
    public void setModifyState(EItemModifyState lastChange) {
        itemModifyState = lastChange;
    }

    /**
     * @return if item is stackable.
     */
    public boolean isStackable() {
        return item.isStackable();
    }

    /**
     * @return if item is dropable.
     */
    public boolean isDropable() {
        return isAugmented() ? false : item.isDropable();
    }

    /**
     * @return if item is destroyable.
     */
    public boolean isDestroyable() {
        return isQuestItem() ? false : item.isDestroyable();
    }

    /**
     * @return if item is tradable
     */
    public boolean isTradable() {
        return isAugmented() ? false : item.isTradable();
    }

    /**
     * @return if item is sellable.
     */
    public boolean isSellable() {
        return isAugmented() ? false : item.isSellable();
    }

    /**
     * @param isPrivateWareHouse : make additionals checks on tradable / shadow items.
     * @return if item can be deposited in warehouse or freight.
     */
    public boolean isDepositable(boolean isPrivateWareHouse) {
        // equipped, hero and quest items
        if (isEquipped() || !item.isDepositable()) { return false; }

        if (!isPrivateWareHouse) {
            // augmented not tradable
            if (!isTradable() || isShadowItem()) { return false; }
        }
        return true;
    }

    /**
     * @return if item is consumable.
     */
    public boolean isConsumable() {
        return item.isConsumable();
    }

    /**
     * @param player           : the player to check.
     * @param allowAdena       : if true, count adenas.
     * @param allowNonTradable : if true, count non tradable items.
     * @return if item is available for manipulation.
     */
    public boolean isAvailable(L2PcInstance player, boolean allowAdena, boolean allowNonTradable) {
        return !isEquipped() // Not equipped
                && (item.getType2() != EItemType2.TYPE2_QUEST) // Not Quest Item
                && (item.getType2() != EItemType2.TYPE2_MONEY || item.getType1() != EItemType1.SHIELD_ARMOR) // not money, not shield
                && (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item of currently summoned pet
                && (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
                && (allowAdena || itemId != 57) // Not adena
                && (player.getCurrentSkill().getSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != itemId) && (!player.isCastingSimultaneouslyNow() || player.getLastSimultaneousSkillCast() == null || player.getLastSimultaneousSkillCast().getItemConsumeId() != itemId) && (allowNonTradable || isTradable());
    }

    @Override
    public void onAction(L2PcInstance player) {
        // Mercenaries tickets case.
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
            html.replace("%ownerid%", _ownerId);
            html.replace("%loc%", _loc.toString());
            html.replace("%class%", getClass().getSimpleName());
            player.sendPacket(html);
        }
        super.onActionShift(player);
    }

    /**
     * @return the level of enchantment of the item.
     */
    public int getEnchantLevel() {
        return _enchantLevel;
    }

    /**
     * Sets the level of enchantment of the item
     *
     * @param enchantLevel : number to apply.
     */
    public void setEnchantLevel(int enchantLevel) {
        if (_enchantLevel == enchantLevel) { return; }

        _enchantLevel = enchantLevel;
        _storedInDb = false;
    }

    /**
     * @return whether this item is augmented or not ; true if augmented.
     */
    public boolean isAugmented() {
        return _augmentation != null;
    }

    /**
     * @return the augmentation object for this item.
     */
    public L2Augmentation getAugmentation() {
        return _augmentation;
    }

    /**
     * Sets a new augmentation.
     *
     * @param augmentation : the augmentation object to apply.
     * @return return true if successfull.
     */
    public boolean setAugmentation(L2Augmentation augmentation) {
        // there shall be no previous augmentation..
        if (_augmentation != null) { return false; }

        _augmentation = augmentation;
        updateItemAttributes(null);
        return true;
    }

    /**
     * Remove the augmentation.
     */
    public void removeAugmentation() {
        if (_augmentation == null) { return; }

        _augmentation = null;

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id = ?");
            statement.setInt(1, getObjectId());
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
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
                if (aug_attributes != -1 && aug_skillId != -1 && aug_skillLevel != -1) { _augmentation = new L2Augmentation(rs.getInt("attributes"), rs.getInt("skill_id"), rs.getInt("skill_level")); }
            }
            rs.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not restore augmentation data for item " + this + " from DB: " + e.getMessage(), e);
        }
    }

    private void updateItemAttributes(Connection pooledCon) {
        try (Connection con = pooledCon == null ? L2DatabaseFactoryOld.getInstance().getConnection() : pooledCon) {
            PreparedStatement statement = con.prepareStatement("REPLACE INTO augmentations VALUES(?,?,?,?)");
            statement.setInt(1, getObjectId());
            if (_augmentation == null) {
                statement.setInt(2, -1);
                statement.setInt(3, -1);
                statement.setInt(4, -1);
            }
            else {
                statement.setInt(2, _augmentation.getAttributes());
                if (_augmentation.getSkill() == null) {
                    statement.setInt(3, 0);
                    statement.setInt(4, 0);
                }
                else {
                    statement.setInt(3, _augmentation.getSkill().getId());
                    statement.setInt(4, _augmentation.getSkill().getLevel());
                }
            }
            statement.executeUpdate();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Could not update attributes for item: " + this + " from DB: ", e);
        }
    }

    /**
     * @return true if this item is a shadow item. Shadow items have a limited life-time.
     */
    public boolean isShadowItem() {
        return _mana >= 0;
    }

    /**
     * Sets the mana for this shadow item.
     *
     * @param period
     * @return return remaining mana of this shadow item
     */
    public int decreaseMana(int period) {
        _storedInDb = false;

        return _mana -= period;
    }

    /**
     * @return the remaining mana of this shadow item (left life-time).
     */
    public int getMana() {
        return _mana / 60;
    }

    /**
     * Returns false cause item can't be attacked
     *
     * @return boolean false
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return false;
    }

    /**
     * This function basically returns a set of functions from L2Item/L2Armor/Weapon, but may add additional functions, if this particular item instance is enhanched for a particular player.
     *
     * @param player : L2Character designating the player
     * @return Func[]
     */
    public List<Func> getStatFuncs(L2Character player) {
        return item.getStatFuncs(this, player);
    }

    /**
     * Updates database.<BR>
     * <BR>
     * <U><I>Concept : </I></U><BR>
     * <B>IF</B> the item exists in database :
     * <UL>
     * <LI><B>IF</B> the item has no owner, or has no location, or has a null quantity : remove item from database</LI>
     * <LI><B>ELSE</B> : update item in database</LI>
     * </UL>
     * <B> Otherwise</B> :
     * <UL>
     * <LI><B>IF</B> the item hasn't a null quantity, and has a correct location, and has a correct owner : insert item in database</LI>
     * </UL>
     */
    public void updateDatabase() {
        _dbLock.lock();

        try {
            if (_existsInDb) {
                if (_ownerId == 0 || _loc == EItemLocation.VOID || (_count == 0 && _loc != EItemLocation.LEASE)) { removeFromDb(); }
                else { updateInDb(); }
            }
            else {
                if (_ownerId == 0 || _loc == EItemLocation.VOID || (_count == 0 && _loc != EItemLocation.LEASE)) { return; }

                insertIntoDb();
            }
        }
        finally {
            _dbLock.unlock();
        }
    }

    /**
     * Init a dropped L2ItemInstance and add it in the world as a visible object.<BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of L2World </B></FONT><BR>
     * <BR>
     *
     * @param dropper : the character who dropped the item.
     * @param x       : X location of the item.
     * @param y       : Y location of the item.
     * @param z       : Z location of the item.
     */
    public final void dropMe(L2Character dropper, int x, int y, int z) {
        ThreadPoolManager.getInstance().executeTask(new ItemDropTask(this, dropper, x, y, z));
    }

    /**
     * Remove a L2ItemInstance from the world and send server->client GetItem packets.<BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
     * <BR>
     *
     * @param player Player that pick up the item
     */
    public final void pickupMe(L2Character player) {
        assert getPosition().getWorldRegion() != null;

        L2WorldRegion oldregion = getPosition().getWorldRegion();

        // Create a server->client GetItem packet to pick up the L2ItemInstance
        GetItem gi = new GetItem(this, player.getObjectId());
        player.broadcastPacket(gi);

        synchronized (this) {
            setIsVisible(false);
            getPosition().setWorldRegion(null);
        }

        // if this item is a mercenary ticket, remove the spawns!
        int itemId = this.itemId;

        if (MercTicketManager.getTicketCastleId(itemId) > 0) { MercTicketManager.getInstance().removeTicket(this); }

        if (!Config.DISABLE_TUTORIAL && (itemId == 57 || itemId == 6353)) {
            L2PcInstance actor = player.getActingPlayer();
            if (actor != null) {
                QuestState qs = actor.getQuestState("Tutorial");
                if (qs != null) { qs.getQuest().notifyEvent("CE" + itemId + "", null, actor); }
            }
        }

        // Remove the L2ItemInstance from the world (out of synchro, to avoid deadlocks)
        L2World.getInstance().removeVisibleObject(this, oldregion);
    }

    /**
     * Update the database with values of the item
     */
    private void updateInDb() {
        assert _existsInDb;

        if (_storedInDb) { return; }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=?,time=? WHERE object_id = ?");
            statement.setInt(1, _ownerId);
            statement.setInt(2, _count);
            statement.setString(3, _loc.name());
            statement.setInt(4, paperdollSlot);
            statement.setInt(5, _enchantLevel);
            statement.setInt(6, _type1);
            statement.setInt(7, _type2);
            statement.setInt(8, _mana);
            statement.setLong(9, _time);
            statement.setInt(10, getObjectId());
            statement.executeUpdate();
            _existsInDb = true;
            _storedInDb = true;
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not update item " + this + " in DB: Reason: " + e.getMessage(), e);
        }
    }

    /**
     * Insert the item in database
     */
    private void insertIntoDb() {
        assert !_existsInDb && getObjectId() != 0;

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left,time) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
            statement.setInt(1, _ownerId);
            statement.setInt(2, itemId);
            statement.setInt(3, _count);
            statement.setString(4, _loc.name());
            statement.setInt(5, paperdollSlot);
            statement.setInt(6, _enchantLevel);
            statement.setInt(7, getObjectId());
            statement.setInt(8, _type1);
            statement.setInt(9, _type2);
            statement.setInt(10, _mana);
            statement.setLong(11, _time);

            statement.executeUpdate();
            _existsInDb = true;
            _storedInDb = true;
            statement.close();

            if (_augmentation != null) { updateItemAttributes(con); }
        }
        catch (Exception e) {
            LOGGER.error("Could not insert item {} into DB: Reason: {}", this, e.getMessage(), e);
        }
    }

    /**
     * Delete item from database
     */
    private void removeFromDb() {
        assert _existsInDb;

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE object_id=?");
            statement.setInt(1, getObjectId());
            statement.executeUpdate();
            _existsInDb = false;
            _storedInDb = false;
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

    /**
     * @return the item in String format.
     */
    @Override
    public String toString() {
        return "" + item;
    }

    public void resetOwnerTimer() {
        if (_itemLootShedule != null) { _itemLootShedule.cancel(true); }

        _itemLootShedule = null;
    }

    public ScheduledFuture<?> getItemLootShedule() {
        return _itemLootShedule;
    }

    public void setItemLootShedule(ScheduledFuture<?> sf) {
        _itemLootShedule = sf;
    }

    public boolean isDestroyProtected() {
        return _destroyProtected;
    }

    public void setDestroyProtected(boolean destroyProtected) {
        _destroyProtected = destroyProtected;
    }

    public boolean isNightLure() {
        return (itemId >= 8505 && itemId <= 8513) || itemId == 8485;
    }

    public boolean getCountDecrease() {
        return _decrease;
    }

    public void setCountDecrease(boolean decrease) {
        _decrease = decrease;
    }

    public int getInitCount() {
        return _initCount;
    }

    public void setInitCount(int InitCount) {
        _initCount = InitCount;
    }

    public void restoreInitCount() {
        if (_decrease) { _count = _initCount; }
    }

    public long getTime() {
        return _time;
    }

    public void setTime(int time) {
        if (time > 0) { _time = time; }
        else { _time = 0; }
    }

    public long getRemainingTime() {
        return _time - System.currentTimeMillis();
    }

    public boolean isPetItem() {
        return item.isPetItem();
    }

    public boolean isPotion() {
        return item.isPotion();
    }

    public boolean isElixir() {
        return item.isElixir();
    }

    public boolean isHerb() {
        return item.getItemType() == EtcItemType.HERB;
    }

    public boolean isHeroItem() {
        return item.isHeroItem();
    }

    public boolean isQuestItem() {
        return item.isQuestItem();
    }

    @Override
    public void decayMe() {
        ItemsOnGroundTaskManager.getInstance().remove(this);

        super.decayMe();
    }

    public void setDropperObjectId(int id) {
        _dropperObjectId = id;
    }

    public final DropProtection getDropProtection() {
        return _dropProtection;
    }

    @Override
    public void sendInfo(L2PcInstance activeChar) {
        if (_dropperObjectId != 0) { activeChar.sendPacket(new DropItem(this, _dropperObjectId)); }
        else { activeChar.sendPacket(new SpawnItem(this)); }
    }

    public List<Quest> getQuestEvents() {
        return item.getQuestEvents();
    }

    @Override
    public boolean isChargedShot(ShotType type) {
        return (_shotsMask & type.getMask()) == type.getMask();
    }

    @Override
    public void setChargedShot(ShotType type, boolean charged) {
        if (charged) { _shotsMask |= type.getMask(); }
        else { _shotsMask &= ~type.getMask(); }
    }

    public void unChargeAllShots() {
        _shotsMask = 0;
    }

    public class ItemDropTask implements Runnable {
        private final L2Character _dropper;
        private final L2ItemInstance _itm;
        private int _x, _y, _z;

        public ItemDropTask(L2ItemInstance item, L2Character dropper, int x, int y, int z) {
            _x = x;
            _y = y;
            _z = z;
            _dropper = dropper;
            _itm = item;
        }

        @Override
        public final void run() {
            assert _itm.getPosition().getWorldRegion() == null;

            if (Config.GEODATA > 0 && _dropper != null) {
                Location dropDest = PathFinding.getInstance().canMoveToTargetLoc(_dropper.getX(), _dropper.getY(), _dropper.getZ(), _x, _y, _z);
                _x = dropDest.getX();
                _y = dropDest.getY();
                _z = dropDest.getZ();
            }

            synchronized (_itm) {
                // Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion
                _itm.setIsVisible(true);
                _itm.getPosition().setWorldPosition(_x, _y, _z);
                _itm.getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
            }

            _itm.getPosition().getWorldRegion().addVisibleObject(_itm);
            _itm.setDropperObjectId(_dropper != null ? _dropper.getObjectId() : 0); // Set the dropper Id for the knownlist packets in sendInfo

            // Add the L2ItemInstance dropped in the world as a visible object
            L2World.getInstance().addVisibleObject(_itm, _itm.getPosition().getWorldRegion());

            ItemsOnGroundTaskManager.getInstance().add(_itm, _dropper);

            _itm.setDropperObjectId(0); // Set the dropper Id back to 0 so it no longer shows the drop packet
        }
    }
}