/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.PetDataTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2PetData;
import net.sf.l2j.gameserver.model.L2PetData.L2PetLevelData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TimeStamp;
import net.sf.l2j.gameserver.model.actor.stat.PetStat;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.taskmanager.ItemsOnGroundTaskManager;
import net.sf.l2j.gameserver.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;

public class L2PetInstance extends L2Summon {
    private int _curFed;
    private final PetInventory _inventory;
    private final int _controlItemId;
    private boolean _respawned;
    private final boolean _mountable;

    private Future<?> _feedTask;

    private L2PetData _data;
    private L2PetLevelData _leveldata;

    /** The Experience before the last Death Penalty */
    private long _expBeforeDeath = 0;
    private int _curWeightPenalty = 0;

    private final Map<Integer, TimeStamp> _reuseTimeStamps = new ConcurrentHashMap<>();

    public Collection<TimeStamp> getReuseTimeStamps() {
        return _reuseTimeStamps.values();
    }

    public Map<Integer, TimeStamp> getReuseTimeStamp() {
        return _reuseTimeStamps;
    }

    public final L2PetLevelData getPetLevelData() {
        if (_leveldata == null) { _leveldata = PetDataTable.getInstance().getPetLevelData(getTemplate().getNpcId(), getStat().getLevel()); }

        return _leveldata;
    }

    public final void setPetData(L2PetLevelData value) {
        _leveldata = value;
    }

    public final L2PetData getPetData() {
        if (_data == null) { _data = PetDataTable.getInstance().getPetData(getTemplate().getNpcId()); }

        return _data;
    }

    /**
     * Manage Feeding Task.
     * <ul>
     * <li>Feed or kill the pet depending on hunger level</li>
     * <li>If pet has food in inventory and feed level drops below 55% then consume food from inventory</li>
     * <li>Send a broadcastStatusUpdate packet for this L2PetInstance</li>
     * </ul>
     */
    class FeedTask implements Runnable {
        @Override
        public void run() {
            try {
                if (getOwner() == null || getOwner().getPet() == null || getOwner().getPet().getObjectId() != getObjectId()) {
                    stopFeed();
                    return;
                }

                // eat
                setCurrentFed((getCurrentFed() > getFeedConsume()) ? getCurrentFed() - getFeedConsume() : 0);

                broadcastStatusUpdate();

                int[] foodIds = getPetData().getFood();
                if (foodIds.length == 0) {
                    if (getCurrentFed() == 0) {
                        getOwner().sendPacket(SystemMessageId.STARVING_GRUMPY_AND_FED_UP_YOUR_PET_HAS_LEFT);
                        deleteMe(getOwner());
                    }
                    else if (isHungry()) { getOwner().sendPacket(SystemMessageId.YOUR_PET_IS_VERY_HUNGRY); }
                    return;
                }

                L2ItemInstance food = null;
                for (int id : foodIds) {
                    food = getInventory().getItemByItemId(id);
                    if (food != null) { break; }
                }

                if (isRunning() && isHungry()) { setWalking(); }
                else if (!isHungry()) { setRunning(); }

                if (food != null && isHungry()) {
                    IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getEtcItem());
                    if (handler != null) {
                        getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY).addItemName(food));
                        handler.useItem(L2PetInstance.this, food, false);
                    }
                }
                else {
                    if (getCurrentFed() == 0) {
                        getOwner().sendPacket(SystemMessageId.YOUR_PET_IS_VERY_HUNGRY);
                        if (Rnd.get(100) < 30) {
                            stopFeed();
                            getOwner().sendPacket(SystemMessageId.STARVING_GRUMPY_AND_FED_UP_YOUR_PET_HAS_LEFT);
                            deleteMe(getOwner());
                        }
                    }
                    else if (getCurrentFed() < (0.10 * getPetLevelData().getPetMaxFeed())) {
                        getOwner().sendPacket(SystemMessageId.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFUL);
                        if (Rnd.get(100) < 3) {
                            stopFeed();
                            getOwner().sendPacket(SystemMessageId.STARVING_GRUMPY_AND_FED_UP_YOUR_PET_HAS_LEFT);
                            deleteMe(getOwner());
                        }
                    }
                }
            }
            catch (Exception e) {
                _log.log(Level.SEVERE, "Pet [ObjectId: " + getObjectId() + "] a feed task error has occurred", e);
            }
        }

        private int getFeedConsume() {
            return (isAttackingNow()) ? getPetLevelData().getPetFeedBattle() : getPetLevelData().getPetFeedNormal();
        }
    }

    public synchronized static L2PetInstance spawnPet(NpcTemplate template, L2PcInstance owner, L2ItemInstance control) {
        if (L2World.getInstance().getPet(owner.getObjectId()) != null) {
            return null; // owner has a pet listed in world
        }

        L2PetInstance pet = restore(control, template, owner);
        // add the pet instance to world
        if (pet != null) {
            pet.setTitle(owner.getName());
            L2World.getInstance().addPet(owner.getObjectId(), pet);
        }

        return pet;
    }

    public L2PetInstance(int objectId, NpcTemplate template, L2PcInstance owner, L2ItemInstance control) {
        super(objectId, template, owner);

        _controlItemId = control.getObjectId();

        if (template.getNpcId() == 12564) { getStat().setLevel((byte) getOwner().getLevel()); }
        else { getStat().setLevel(template.getLevel()); }

        _inventory = new PetInventory(this);
        _inventory.restore();

        _mountable = PetDataTable.isMountable(template.getNpcId());
    }

    @Override
    public void initCharStat() {
        setStat(new PetStat(this));
    }

    @Override
    public PetStat getStat() {
        return (PetStat) super.getStat();
    }

    public boolean isRespawned() {
        return _respawned;
    }

    @Override
    public int getSummonType() {
        return 2;
    }

    @Override
    public void onAction(L2PcInstance player) {
        boolean isOwner = player.getObjectId() == getOwner().getObjectId();
        if (isOwner && player != getOwner()) { updateRefOwner(player); }

        super.onAction(player);
    }

    @Override
    public int getControlItemId() {
        return _controlItemId;
    }

    public L2ItemInstance getControlItem() {
        return getOwner().getInventory().getItemByObjectId(_controlItemId);
    }

    public int getCurrentFed() {
        return _curFed;
    }

    public void setCurrentFed(int num) {
        _curFed = num > getMaxFed() ? getMaxFed() : num;
    }

    /**
     * Returns the pet's currently equipped weapon instance (if any).
     */
    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        return _inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND);
    }

    /**
     * Returns the pet's currently equipped weapon (if any).
     */
    @Override
    public Weapon getActiveWeaponItem() {
        L2ItemInstance weapon = getActiveWeaponInstance();
        if (weapon == null) { return null; }

        return (Weapon) weapon.getItem();
    }

    @Override
    public PetInventory getInventory() {
        return _inventory;
    }

    /**
     * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param objectId    : int Item Instance identifier of the item to be destroyed
     * @param count       : int Quantity of items to be destroyed
     * @param reference   : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);

        if (item == null) {
            if (sendMessage) { getOwner().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }

            return false;
        }

        // Send Pet inventory update packet
        PetInventoryUpdate petIU = new PetInventoryUpdate();
        petIU.addItem(item);
        getOwner().sendPacket(petIU);

        if (sendMessage) {
            if (count > 1) { getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item.getItemId()).addItemNumber(count)); }
            else { getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item.getItemId())); }
        }
        return true;
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param itemId      : int Item identifier of the item to be destroyed
     * @param count       : int Quantity of items to be destroyed
     * @param reference   : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);

        if (item == null) {
            if (sendMessage) { getOwner().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }

            return false;
        }

        // Send Pet inventory update packet
        PetInventoryUpdate petIU = new PetInventoryUpdate();
        petIU.addItem(item);
        getOwner().sendPacket(petIU);

        if (sendMessage) {
            if (count > 1) { getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item.getItemId()).addItemNumber(count)); }
            else { getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item.getItemId())); }
        }
        return true;
    }

    @Override
    public void doPickupItem(L2Object object) {
        if (isDead()) { return; }

        getAI().setIntention(EIntention.IDLE);

        if (!(object instanceof L2ItemInstance)) {
            // dont try to pickup anything that is not an item :)
            _log.warning(getName() + " tried to pickup a wrong target: " + object);
            return;
        }

        broadcastPacket(new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading()));
        L2ItemInstance target = (L2ItemInstance) object;

        // Cursed weapons
        if (CursedWeaponsManager.getInstance().isCursed(target.getItemId())) {
            SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
            smsg.addItemName(target.getItemId());
            getOwner().sendPacket(smsg);
            return;
        }

        synchronized (target) {
            if (!target.isVisible()) { return; }

            if (!target.getDropProtection().tryPickUp(this)) {
                getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
                return;
            }

            if (!_inventory.validateCapacity(target)) {
                getOwner().sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
                return;
            }

            if (!_inventory.validateWeight(target, target.getCount())) {
                getOwner().sendPacket(SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED);
                return;
            }

            if (target.getOwnerId() != 0 && target.getOwnerId() != getOwner().getObjectId() && !getOwner().isInLooterParty(target.getOwnerId())) {
                if (target.getItemId() == 57) {
                    SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
                    smsg.addNumber(target.getCount());
                    getOwner().sendPacket(smsg);
                }
                else if (target.getCount() > 1) {
                    SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
                    smsg.addItemName(target.getItemId());
                    smsg.addNumber(target.getCount());
                    getOwner().sendPacket(smsg);
                }
                else {
                    SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
                    smsg.addItemName(target.getItemId());
                    getOwner().sendPacket(smsg);
                }
                return;
            }

            if (target.getItemLootShedule() != null && (target.getOwnerId() == getOwner().getObjectId() || getOwner().isInLooterParty(target.getOwnerId()))) { target.resetOwnerTimer(); }

            // If owner is in party and it isnt finders keepers, distribute the item instead of stealing it -.-
            if (getOwner().isInParty() && getOwner().getParty().getLootDistribution() != L2Party.ITEM_LOOTER) { getOwner().getParty().distributeItem(getOwner(), target); }
            else { target.pickupMe(this); }

            // Item must be removed from ItemsOnGroundManager if it is active.
            ItemsOnGroundTaskManager.getInstance().remove(target);
        }

        // Auto use herbs - pick up
        if (target.getItemType() == EtcItemType.HERB) {
            IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getEtcItem());
            if (handler != null) { handler.useItem(this, target, false); }

            ItemTable.getInstance().destroyItem("Consume", target, getOwner(), null);
            broadcastStatusUpdate();
        }
        else {
            // if item is instance of L2ArmorType or WeaponType broadcast an "Attention" system message
            if (target.getItemType() instanceof ArmorType || target.getItemType() instanceof WeaponType) {
                SystemMessage msg;
                if (target.getEnchantLevel() > 0) {
                    msg = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PET_PICKED_UP_S2_S3);
                    msg.addPcName(getOwner());
                    msg.addNumber(target.getEnchantLevel());
                    msg.addItemName(target.getItemId());
                }
                else {
                    msg = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PET_PICKED_UP_S2);
                    msg.addPcName(getOwner());
                    msg.addItemName(target.getItemId());
                }
                getOwner().broadcastPacket(msg, 1400);
            }

            SystemMessage sm2;
            if (target.getItemId() == 57) {
                sm2 = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1_ADENA);
                sm2.addItemNumber(target.getCount());
            }
            else if (target.getEnchantLevel() > 0) {
                sm2 = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1_S2);
                sm2.addNumber(target.getEnchantLevel());
                sm2.addItemName(target.getItemId());
            }
            else if (target.getCount() > 1) {
                sm2 = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S2_S1_S);
                sm2.addItemName(target.getItemId());
                sm2.addItemNumber(target.getCount());
            }
            else {
                sm2 = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1);
                sm2.addItemName(target.getItemId());
            }
            getOwner().sendPacket(sm2);
            _inventory.addItem("Pickup", target, getOwner(), this);
            getOwner().sendPacket(new PetItemList(this));
        }

        getAI().setIntention(EIntention.IDLE);

        if (getFollowStatus()) { followOwner(); }
    }

    @Override
    public void deleteMe(L2PcInstance owner) {
        _inventory.deleteMe();
        super.deleteMe(owner);
        destroyControlItem(owner); // this should also delete the pet from the db
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) { return false; }

        stopFeed();
        getOwner().sendPacket(SystemMessageId.MAKE_SURE_YOU_RESSURECT_YOUR_PET_WITHIN_20_MINUTES);
        DecayTaskManager.getInstance().add(this, 1200);

        // Dont decrease exp if killed in duel or arena
        L2PcInstance owner = getOwner();
        if (owner != null && !owner.isInDuel() && (!isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE))) { deathPenalty(); }

        return true;
    }

    @Override
    public void doRevive() {
        getOwner().removeReviving();

        super.doRevive();

        // stopDecay
        DecayTaskManager.getInstance().cancel(this);
        startFeed();

        if (!isHungry()) { setRunning(); }

        getAI().setIntention(EIntention.ACTIVE, null);
    }

    @Override
    public void doRevive(double revivePower) {
        // Restore the pet's lost experience depending on the % return of the skill used
        restoreExp(revivePower);
        doRevive();
    }

    /**
     * Transfers item to another inventory
     *
     * @param process   : String Identifier of process triggering this action
     * @param objectId  : ObjectId of the item to be transfered
     * @param count     : int Quantity of items to be transfered
     * @param target    : The Inventory to target
     * @param actor     : L2PcInstance Player requesting the item transfer
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item in inventory
     */
    public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2PcInstance actor, L2Object reference) {
        final L2ItemInstance oldItem = checkItemManipulation(objectId, count);
        if (oldItem == null) { return null; }

        final L2ItemInstance newItem = _inventory.transferItem(process, objectId, count, target, actor, reference);
        if (newItem == null) { return null; }

        // Send pet inventory update packet
        PetInventoryUpdate petIU = new PetInventoryUpdate();
        if (oldItem.getCount() > 0 && oldItem != newItem) { petIU.addModifiedItem(oldItem); }
        else { petIU.addRemovedItem(oldItem); }
        sendPacket(petIU);

        // Update pet current load aswell
        updateAndBroadcastStatus(1);

        // Send player inventory update packet
        InventoryUpdate playerIU = new InventoryUpdate();
        if (newItem.getCount() > count) { playerIU.addModifiedItem(newItem); }
        else { playerIU.addNewItem(newItem); }
        sendPacket(playerIU);

        // Update player current load aswell
        StatusUpdate playerSU = new StatusUpdate(getOwner());
        playerSU.addAttribute(StatusUpdate.CUR_LOAD, getOwner().getCurrentLoad());
        sendPacket(playerSU);

        return newItem;
    }

    public L2ItemInstance checkItemManipulation(int objectId, int count) {
        final L2ItemInstance item = _inventory.getItemByObjectId(objectId);
        if (item == null) { return null; }

        if (count < 1 || (count > 1 && !item.isStackable())) { return null; }

        if (count > item.getCount()) { return null; }

        return item;
    }

    /**
     * Remove the Pet from DB and its associated item from the player inventory
     *
     * @param owner The owner from whose invenory we should delete the item
     */
    public void destroyControlItem(L2PcInstance owner) {
        // remove the pet instance from world
        L2World.getInstance().removePet(owner.getObjectId());

        // delete from inventory
        try {
            L2ItemInstance removedItem = owner.getInventory().destroyItem("PetDestroy", _controlItemId, 1, getOwner(), this);

            if (removedItem == null) { _log.warning("Couldn't destroy petControlItem for " + owner.getName() + ", pet: " + this); }
            else {
                InventoryUpdate iu = new InventoryUpdate();
                iu.addRemovedItem(removedItem);

                owner.sendPacket(iu);

                StatusUpdate su = new StatusUpdate(owner);
                su.addAttribute(StatusUpdate.CUR_LOAD, owner.getCurrentLoad());
                owner.sendPacket(su);

                owner.broadcastUserInfo();

                L2World.getInstance().removeObject(removedItem);
            }
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Error while destroying control item: " + e.getMessage(), e);
        }

        // pet control item no longer exists, delete the pet from the db
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
            statement.setInt(1, _controlItemId);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            _log.log(Level.SEVERE, "Failed to delete Pet [ObjectId: " + getObjectId() + "]", e);
        }
    }

    /** @return Returns the mountable. */
    @Override
    public boolean isMountable() {
        return _mountable;
    }

    private static L2PetInstance restore(L2ItemInstance control, NpcTemplate template, L2PcInstance owner) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            L2PetInstance pet;
            if (template.isType("L2BabyPet")) { pet = new L2BabyPetInstance(IdFactory.getInstance().getNextId(), template, owner, control); }
            else { pet = new L2PetInstance(IdFactory.getInstance().getNextId(), template, owner, control); }

            PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed FROM pets WHERE item_obj_id=?");
            statement.setInt(1, control.getObjectId());
            ResultSet rset = statement.executeQuery();
            if (!rset.next()) {
                rset.close();
                statement.close();
                return pet;
            }

            pet._respawned = true;
            pet.setName(rset.getString("name"));

            pet.getStat().setLevel(rset.getByte("level"));
            pet.getStat().setExp(rset.getLong("exp"));
            pet.getStat().setSp(rset.getInt("sp"));

            pet.getStatus().setCurrentHp(rset.getDouble("curHp"));
            pet.getStatus().setCurrentMp(rset.getDouble("curMp"));
            pet.getStatus().setCurrentCp(pet.getMaxCp());
            if (rset.getDouble("curHp") < 0.5) {
                pet.setIsDead(true);
                pet.stopHpMpRegeneration();
            }

            pet.setCurrentFed(rset.getInt("fed"));

            rset.close();
            statement.close();
            return pet;
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Could not restore pet data for owner: " + owner + " - " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void store() {
        if (_controlItemId == 0) { return; }

        String req;
        if (!_respawned) { req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,item_obj_id) VALUES (?,?,?,?,?,?,?,?)"; }
        else { req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=? WHERE item_obj_id = ?"; }

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(req);
            statement.setString(1, getName());
            statement.setInt(2, getStat().getLevel());
            statement.setDouble(3, getStatus().getCurrentHp());
            statement.setDouble(4, getStatus().getCurrentMp());
            statement.setLong(5, getStat().getExp());
            statement.setInt(6, getStat().getSp());
            statement.setInt(7, _curFed);
            statement.setInt(8, _controlItemId);
            statement.executeUpdate();
            statement.close();
            _respawned = true;
        }
        catch (Exception e) {
            _log.log(Level.SEVERE, "Failed to store Pet [ObjectId: " + getObjectId() + "] data", e);
        }

        L2ItemInstance itemInst = getControlItem();
        if (itemInst != null && itemInst.getEnchantLevel() != getStat().getLevel()) {
            itemInst.setEnchantLevel(getStat().getLevel());
            itemInst.updateDatabase();
        }
    }

    public synchronized void stopFeed() {
        if (_feedTask != null) {
            _feedTask.cancel(false);
            _feedTask = null;
        }
    }

    public synchronized void startFeed() {
        // stop feeding task if its active
        stopFeed();

        if (!isDead() && getOwner().getPet() == this) { _feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000); }
    }

    @Override
    public synchronized void unSummon(L2PcInstance owner) {
        // First, stop feed task.
        stopFeed();

        // Then drop inventory.
        if (!isDead()) {
            if (_inventory != null) {
                _inventory.deleteMe(); }
        }

        // Finally drop pet itself.
        super.unSummon(owner);

        // Drop pet from world's pet list.
        if (!isDead()) { L2World.getInstance().removePet(owner.getObjectId()); }
    }

    /**
     * Restore the specified % of experience this L2PetInstance has lost.
     *
     * @param restorePercent
     */
    public void restoreExp(double restorePercent) {
        if (_expBeforeDeath > 0) {
            // Restore the specified % of lost experience.
            getStat().addExp(Math.round((_expBeforeDeath - getStat().getExp()) * restorePercent / 100));
            _expBeforeDeath = 0;
        }
    }

    private void deathPenalty() {
        int lvl = getStat().getLevel();
        double percentLost = -0.07 * lvl + 6.5;

        // Calculate the Experience loss
        long lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);

        // Get the Experience before applying penalty
        _expBeforeDeath = getStat().getExp();

        // Set the new Experience value of the L2PetInstance
        getStat().addExp(-lostExp);
    }

    @Override
    public void addExpAndSp(long addToExp, int addToSp) {
        getStat().addExpAndSp(Math.round(addToExp * ((getNpcId() == 12564) ? Config.SINEATER_XP_RATE : Config.PET_XP_RATE)), addToSp);
    }

    @Override
    public long getExpForThisLevel() {
        return getStat().getExpForLevel(getLevel());
    }

    @Override
    public long getExpForNextLevel() {
        return getStat().getExpForLevel(getLevel() + 1);
    }

    @Override
    public final int getLevel() {
        return getStat().getLevel();
    }

    public int getMaxFed() {
        return getStat().getMaxFeed();
    }

    @Override
    public int getAccuracy() {
        return getStat().getAccuracy();
    }

    @Override
    public int getCriticalHit(L2Character target, L2Skill skill) {
        return getStat().getCriticalHit(target, skill);
    }

    @Override
    public int getEvasionRate(L2Character target) {
        return getStat().getEvasionRate(target);
    }

    @Override
    public int getRunSpeed() {
        return getStat().getRunSpeed();
    }

    @Override
    public int getPAtkSpd() {
        return getStat().getPAtkSpd();
    }

    @Override
    public int getMAtkSpd() {
        return getStat().getMAtkSpd();
    }

    @Override
    public int getMAtk(L2Character target, L2Skill skill) {
        return getStat().getMAtk(target, skill);
    }

    @Override
    public int getMDef(L2Character target, L2Skill skill) {
        return getStat().getMDef(target, skill);
    }

    @Override
    public int getPAtk(L2Character target) {
        return getStat().getPAtk(target);
    }

    @Override
    public int getPDef(L2Character target) {
        return getStat().getPDef(target);
    }

    @Override
    public final int getSkillLevel(int skillId) {
        // Unknown skill. Return -1.
        if (getKnownSkill(skillId) == null) { return -1; }

        // Max level for pet is 80, max level for pet skills is 12 => ((80 - 8) / 6) = 12.
        return Math.max(1, Math.min((getLevel() - 8) / 6, SkillTable.getInstance().getMaxLevel(skillId)));
    }

    public void updateRefOwner(L2PcInstance owner) {
        int oldOwnerId = getOwner().getObjectId();

        setOwner(owner);
        L2World.getInstance().removePet(oldOwnerId);
        L2World.getInstance().addPet(oldOwnerId, this);
    }

    public int getCurrentLoad() {
        return _inventory.getTotalWeight();
    }

    @Override
    public final int getMaxLoad() {
        return getPetData().getLoad();
    }

    @Override
    public int getSoulShotsPerHit() {
        return (getLevel() < 40) ? 1 : 2;
    }

    @Override
    public int getSpiritShotsPerHit() {
        return (getLevel() < 40) ? 1 : 2;
    }

    public int getInventoryLimit() {
        return Config.INVENTORY_MAXIMUM_PET;
    }

    public void refreshOverloaded() {
        int maxLoad = getMaxLoad();
        if (maxLoad > 0) {
            int weightproc = getCurrentLoad() * 1000 / maxLoad;
            int newWeightPenalty;

            if (weightproc < 500) { newWeightPenalty = 0; }
            else if (weightproc < 666) { newWeightPenalty = 1; }
            else if (weightproc < 800) { newWeightPenalty = 2; }
            else if (weightproc < 1000) { newWeightPenalty = 3; }
            else { newWeightPenalty = 4; }

            if (_curWeightPenalty != newWeightPenalty) {
                _curWeightPenalty = newWeightPenalty;
                if (newWeightPenalty > 0) {
                    addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
                    setIsOverloaded(getCurrentLoad() >= maxLoad);
                }
                else {
                    super.removeSkill(getKnownSkill(4270));
                    setIsOverloaded(false);
                }
            }
        }
    }

    @Override
    public void updateAndBroadcastStatus(int val) {
        refreshOverloaded();
        super.updateAndBroadcastStatus(val);
    }

    /**
     * A simple check, made to see if this current pet is hungry.<br>
     * <br>
     * If the actual amount of food < 55% of the max, the pet is shown as hungry. Both atkspd and cstspd are divided by 2, and deluxe food can be used automatically if worn.
     **/
    @Override
    public final boolean isHungry() {
        return (_curFed < (getMaxFed() * 0.55));
    }

    public boolean canEatFoodId(int itemId) {
        return Util.contains(_data.getFood(), itemId);
    }

    public boolean canWear(Item item) {
        if (PetDataTable.isHatchling(getNpcId()) && item.getBodyPart() == Item.SLOT_HATCHLING) { return true; }

        if (PetDataTable.isWolf(getNpcId()) && item.getBodyPart() == Item.SLOT_WOLF) { return true; }

        if (PetDataTable.isStrider(getNpcId()) && item.getBodyPart() == Item.SLOT_STRIDER) { return true; }

        if (PetDataTable.isBaby(getNpcId()) && item.getBodyPart() == Item.SLOT_BABYPET) { return true; }

        return false;
    }

    @Override
    public final int getWeapon() {
        L2ItemInstance weapon = _inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND);
        if (weapon != null) { return weapon.getItemId(); }

        return 0;
    }

    @Override
    public final int getArmor() {
        L2ItemInstance weapon = _inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
        if (weapon != null) { return weapon.getItemId(); }

        return 0;
    }

    @Override
    public void setName(String name) {
        L2ItemInstance controlItem = getControlItem();
        if (controlItem.getCustomType2() == (name == null ? 1 : 0)) {
            // Name isn't setted yet.
            controlItem.setCustomType2(name != null ? 1 : 0);
            controlItem.updateDatabase();

            InventoryUpdate iu = new InventoryUpdate();
            iu.addModifiedItem(controlItem);
            getOwner().sendPacket(iu);
        }
        super.setName(name);
    }

    /**
     * Index according to skill id the current timestamp of use.
     *
     * @param skill
     * @param reuse delay
     */
    @Override
    public void addTimeStamp(L2Skill skill, long reuse) {
        _reuseTimeStamps.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse));
    }
}