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
import net.sf.l2j.L2DatabaseFactoryOld;
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
import net.sf.l2j.gameserver.model.TimeStamp;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.stat.PetStat;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.*;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.EWeaponType;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.skill.L2Skill;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@SuppressWarnings("ObjectEquality")
public class L2PetInstance extends L2Summon {
    private final PetInventory inventory;

    private int _curFed;
    private final int controlItemId;
    private boolean _respawned;
    private final boolean mountable;

    private Future<?> _feedTask;

    private L2PetData _data;
    private L2PetLevelData _leveldata;

    /** The Experience before the last Death Penalty */
    private long _expBeforeDeath;
    private int _curWeightPenalty;

    private final Map<Integer, TimeStamp> reuseTimeStamps = new ConcurrentHashMap<>();

    public final L2PetLevelData getPetLevelData() {
        if (_leveldata == null) {
            _leveldata = PetDataTable.getInstance().getPetLevelData(getTemplate().getNpcId(), getStat().getLevel());
        }
        return _leveldata;
    }

    public final void setPetData(L2PetLevelData value) { _leveldata = value; }

    public final L2PetData getPetData() {
        if (_data == null) {
            _data = PetDataTable.getInstance().getPetData(getTemplate().getNpcId());
        }
        return _data;
    }

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
            catch (RuntimeException e) {
                LOGGER.error("Pet [ObjectId: {}] a feed task error has occurred", getObjectId(), e);
            }
        }

        private int getFeedConsume() {
            return (isAttackingNow()) ? getPetLevelData().getPetFeedBattle() : getPetLevelData().getPetFeedNormal();
        }
    }

    public static synchronized L2PetInstance spawnPet(NpcTemplate template, L2PcInstance owner, L2ItemInstance control) {
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

        controlItemId = control.getObjectId();

        if (template.getNpcId() == 12564) {
            getStat().setLevel((byte) getOwner().getLevel());
        }
        else {
            getStat().setLevel(template.getLevel());
        }

        inventory = new PetInventory(this);
        inventory.restore();

        mountable = PetDataTable.isMountable(template.getNpcId());
    }

    @Override
    public void initCharStat() { setStat(new PetStat(this)); }

    @Override
    public PetStat getStat() { return (PetStat) super.getStat(); }

    public boolean isRespawned() { return _respawned; }

    @Override
    public int getSummonType() { return 2; }

    @Override
    public void onAction(L2PcInstance player) {
        boolean isOwner = player.getObjectId() == getOwner().getObjectId();
        if (isOwner && player != getOwner()) { updateRefOwner(player); }
        super.onAction(player);
    }

    @Override
    public int getControlItemId() { return controlItemId; }

    public L2ItemInstance getControlItem() {
        return getOwner().getInventory().getItemByObjectId(controlItemId);
    }

    public int getCurrentFed() { return _curFed; }

    public void setCurrentFed(int num) { _curFed = num > getMaxFed() ? getMaxFed() : num; }

    @Override
    public L2ItemInstance getActiveWeaponInstance() { return inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND); }

    @Override
    public Weapon getActiveWeaponItem() {
        L2ItemInstance weapon = getActiveWeaponInstance();
        if (weapon == null) { return null; }
        return (Weapon) weapon.getItem();
    }

    @Override
    public PetInventory getInventory() { return inventory; }

    @Override
    public boolean destroyItem(EItemProcessPurpose process, int objectId, int count, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = inventory.destroyItem(process, objectId, count, getOwner(), reference, sendMessage);
        if (item == null) {
            if (sendMessage) {
                getOwner().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
            }
            return false;
        }

        PetInventoryUpdate petIU = new PetInventoryUpdate();
        petIU.addItem(item);
        getOwner().sendPacket(petIU);

        return true;
    }

    @Override
    public boolean destroyItemByItemId(EItemProcessPurpose process, int itemId, int count, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference, sendMessage);
        if (item == null) {
            if (sendMessage) {
                getOwner().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
            }
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
            LOGGER.warn("{} tried to pickup a wrong target: {}", getName(), object);
            return;
        }

        broadcastPacket(new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading()));
        L2ItemInstance target = (L2ItemInstance) object;

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

            if (!inventory.validateCapacity(target)) {
                getOwner().sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
                return;
            }

            if (!inventory.validateWeight(target, target.getCount())) {
                getOwner().sendPacket(SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED);
                return;
            }

            if (target.getOwnerId() != 0 && target.getOwnerId() != getOwner().getObjectId() && !getOwner().isInLooterParty(target.getOwnerId())) {
                if (target.getItemId() == ItemConst.ADENA_ID) {
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

            if (target.getItemLootTask() != null && (target.getOwnerId() == getOwner().getObjectId() || getOwner().isInLooterParty(target.getOwnerId()))) { target.resetOwnerTimer(); }

            if (getOwner().isInParty() && getOwner().getParty().getLootDistribution() != L2Party.ITEM_LOOTER) { getOwner().getParty().distributeItem(getOwner(), target); }
            else { target.pickupMe(this); }

            ItemsOnGroundTaskManager.getInstance().remove(target);
        }

        if (target.getItemType() == EtcItemType.HERB) {
            IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getEtcItem());
            if (handler != null) { handler.useItem(this, target, false); }
            ItemTable.getInstance().destroyItem(EItemProcessPurpose.CONSUME, target, getOwner(), null);
            broadcastStatusUpdate();
        }
        else {
            // if item is instance of L2ArmorType or EWeaponType broadcast an "Attention" system message
            if (target.getItemType() instanceof ArmorType || target.getItemType() instanceof EWeaponType) {
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
            if (target.getItemId() == ItemConst.ADENA_ID) {
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
            inventory.addItem(EItemProcessPurpose.PICKUP, target, getOwner(), this);
            getOwner().sendPacket(new PetItemList(this));
        }
        getAI().setIntention(EIntention.IDLE);
        if (getFollowStatus()) {
            followOwner();
        }
    }

    @Override
    public void deleteMe(L2PcInstance owner) {
        inventory.deleteMe();
        super.deleteMe(owner);
        destroyControlItem(owner); // this should also delete the pet from the db
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) { return false; }
        stopFeed();
        getOwner().sendPacket(SystemMessageId.MAKE_SURE_YOU_RESSURECT_YOUR_PET_WITHIN_20_MINUTES);
        DecayTaskManager.getInstance().add(this, 1200);
        L2PcInstance owner = getOwner();
        if (owner != null && !owner.isInDuel() && (!isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE))) {
            deathPenalty();
        }
        return true;
    }

    @Override
    public void doRevive() {
        getOwner().removeReviving();
        super.doRevive();
        DecayTaskManager.getInstance().cancel(this);
        startFeed();
        if (!isHungry()) {
            setRunning();
        }
        getAI().setIntention(EIntention.ACTIVE, null);
    }

    @Override
    public void doRevive(double revivePower) {
        restoreExp(revivePower);
        doRevive();
    }

    public L2ItemInstance transferItem(EItemProcessPurpose process, int objectId, int count, Inventory target, L2PcInstance actor, L2Object reference) {
        L2ItemInstance oldItem = checkItemManipulation(objectId, count);
        if (oldItem == null) { return null; }

        L2ItemInstance newItem = inventory.transferItem(process, objectId, count, target, actor, reference);
        if (newItem == null) { return null; }

        PetInventoryUpdate petIU = new PetInventoryUpdate();
        if (oldItem.getCount() > 0 && oldItem != newItem) { petIU.addModifiedItem(oldItem); }
        else { petIU.addRemovedItem(oldItem); }
        sendPacket(petIU);

        updateAndBroadcastStatus(1);

        InventoryUpdate playerIU = new InventoryUpdate();
        if (newItem.getCount() > count) { playerIU.addModifiedItem(newItem); }
        else { playerIU.addNewItem(newItem); }
        sendPacket(playerIU);

        StatusUpdate playerSU = new StatusUpdate(getOwner());
        playerSU.addAttribute(StatusUpdate.CUR_LOAD, getOwner().getCurrentLoad());
        sendPacket(playerSU);

        return newItem;
    }

    public L2ItemInstance checkItemManipulation(int objectId, int count) {
        L2ItemInstance item = inventory.getItemByObjectId(objectId);
        if (item == null) { return null; }
        if (count < 1 || (count > 1 && !item.isStackable())) { return null; }
        if (count > item.getCount()) { return null; }
        return item;
    }

    public void destroyControlItem(L2PcInstance owner) {
        L2World.getInstance().removePet(owner.getObjectId());
        try {
            L2ItemInstance removedItem = owner.getInventory().destroyItem(EItemProcessPurpose.PET_DESTROY, controlItemId, 1, getOwner(), this, true);
            if (removedItem == null) {
                LOGGER.warn("Couldn't destroy petControlItem for {}, pet: {}", owner.getName(), this);
            }
            else {
                owner.broadcastUserInfo();
                L2World.getInstance().removeObject(removedItem);
            }
        }
        catch (RuntimeException e) {
            LOGGER.error("Error while destroying control item: {}", e.getMessage(), e);
        }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
            statement.setInt(1, controlItemId);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Failed to delete Pet [ObjectId: {}]", getObjectId(), e);
        }
    }

    @Override
    public boolean isMountable() { return mountable; }

    private static L2PetInstance restore(L2ItemInstance control, NpcTemplate template, L2PcInstance owner) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
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
            LOGGER.error("Could not restore pet data for owner: {} - {}", owner, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void store() {
        if (controlItemId == 0) { return; }

        String req;
        if (!_respawned) { req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,item_obj_id) VALUES (?,?,?,?,?,?,?,?)"; }
        else { req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=? WHERE item_obj_id = ?"; }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(req);
            statement.setString(1, getName());
            statement.setInt(2, getStat().getLevel());
            statement.setDouble(3, getStatus().getCurrentHp());
            statement.setDouble(4, getStatus().getCurrentMp());
            statement.setLong(5, getStat().getExp());
            statement.setInt(6, getStat().getSp());
            statement.setInt(7, _curFed);
            statement.setInt(8, controlItemId);
            statement.executeUpdate();
            statement.close();
            _respawned = true;
        }
        catch (Exception e) {
            LOGGER.error("Failed to store Pet [ObjectId: {}] data", getObjectId(), e);
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
        stopFeed();
        if (!isDead() && getOwner().getPet() == this) {
            _feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
        }
    }

    @Override
    public synchronized void unSummon(L2PcInstance owner) {
        stopFeed();
        if (!isDead()) {
            if (inventory != null) {
                inventory.deleteMe();
            }
        }
        super.unSummon(owner);
        if (!isDead()) {
            L2World.getInstance().removePet(owner.getObjectId());
        }
    }

    public void restoreExp(double restorePercent) {
        if (_expBeforeDeath > 0) {
            getStat().addExp(Math.round((_expBeforeDeath - getStat().getExp()) * restorePercent / 100));
            _expBeforeDeath = 0;
        }
    }

    private void deathPenalty() {
        int lvl = getStat().getLevel();
        double percentLost = -0.07 * lvl + 6.5;
        long lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
        _expBeforeDeath = getStat().getExp();
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
        if (getKnownSkill(skillId) == null) { return -1; }
        return Math.max(1, Math.min((getLevel() - 8) / 6, SkillTable.getMaxLevel(skillId)));
    }

    public void updateRefOwner(L2PcInstance owner) {
        int oldOwnerId = getOwner().getObjectId();
        setOwner(owner);
        L2World.getInstance().removePet(oldOwnerId);
        L2World.getInstance().addPet(oldOwnerId, this);
    }

    public int getCurrentLoad() {
        return inventory.getTotalWeight();
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
                    addSkill(SkillTable.getInfo(4270, newWeightPenalty));
                    setIsOverloaded(getCurrentLoad() >= maxLoad);
                }
                else {
                    removeSkill(getKnownSkill(4270));
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

    @Override
    public final boolean isHungry() {
        return _curFed < (getMaxFed() * 0.55);
    }

    public boolean canEatFoodId(int itemId) {
        return Util.contains(_data.getFood(), itemId);
    }

    public boolean canWear(Item item) {
        if (PetDataTable.isHatchling(getNpcId()) && item.getBodyPart() == EItemBodyPart.SLOT_HATCHLING) { return true; }
        if (PetDataTable.isWolf(getNpcId()) && item.getBodyPart() == EItemBodyPart.SLOT_WOLF) { return true; }
        if (PetDataTable.isStrider(getNpcId()) && item.getBodyPart() == EItemBodyPart.SLOT_STRIDER) { return true; }
        return PetDataTable.isBaby(getNpcId()) && item.getBodyPart() == EItemBodyPart.SLOT_BABYPET;
    }

    @Override
    public final int getWeapon() {
        L2ItemInstance weapon = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND);
        if (weapon != null) { return weapon.getItemId(); }

        return 0;
    }

    @Override
    public final int getArmor() {
        L2ItemInstance weapon = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
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

    @Override
    public void addTimeStamp(L2Skill skill, long reuse) {
        reuseTimeStamps.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse));
    }
}