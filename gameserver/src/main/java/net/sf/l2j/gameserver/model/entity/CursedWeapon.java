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
package net.sf.l2j.gameserver.model.entity;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Party.MessageType;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.Earthquake;
import net.sf.l2j.gameserver.network.client.game_to_client.ExRedSky;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.network.client.game_to_client.UserInfo;
import net.sf.l2j.gameserver.util.Broadcast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CursedWeapon {
    private static final Logger _log = Logger.getLogger(CursedWeapon.class.getName());

    private final String _name;

    private final int _itemId;
    private L2ItemInstance _item = null;

    private int _playerId = 0;
    protected L2PcInstance _player = null;

    // Skill id and max level. Max level is took from skillid (allow custom skills).
    private final int _skillId;
    private final int _skillMaxLevel;

    // Drop rate (when a mob is killed) and chance of dissapear (when a CW owner dies).
    private int _dropRate;
    private int _disapearChance;

    // Overall duration (in hours) and hungry - used for daily task - duration (in hours)
    private int _duration;
    private int _durationLost;

    // Basic number used to calculate next number of needed victims for a stage (50% to 150% the given value).
    private int _stageKills;

    private boolean _isDropped = false;
    private boolean _isActivated = false;

    private ScheduledFuture<?> _overallTimerTask;
    private ScheduledFuture<?> _dailyTimerTask;
    private ScheduledFuture<?> _dropTimerTask;

    private int _playerKarma = 0;
    private int _playerPkKills = 0;

    // Number of current killed, current stage of weapon (1 by default, max is _skillMaxLevel), and number of victims needed for next stage.
    private int _nbKills = 0;
    private int _currentStage = 1;
    private int _numberBeforeNextStage = 0;

    // Hungry timer (in minutes) and overall end timer (in ms).
    protected int _hungryTime = 0;
    protected long _endTime = 0;

    public CursedWeapon(int itemId, int skillId, String name) {
        _name = name;
        _itemId = itemId;
        _skillId = skillId;
        _skillMaxLevel = SkillTable.getMaxLevel(_skillId);
    }

    /**
     * This method is used to destroy a CW.<br>
     * It manages following states :
     * <ul>
     * <li><u>item on a online player</u> : drops the CW from inventory, and set back ancient pk/karma values.</li>
     * <li><u>item on a offline player</u> : make SQL operations in order to drop item from inventory.</li>
     * <li><u>item on ground</u> : destroys the item directly.</li>
     * </ul>
     * For all cases, a message is broadcasted, and the different states are reinitialized.
     */
    public void endOfLife() {
        if (_isActivated) {
            // Player is online ; unequip weapon && destroy it.
            if (_player != null && _player.isOnline()) {
                _log.info(_name + " being removed online.");

                _player.abortAttack();

                _player.setKarma(_playerKarma);
                _player.setPkKills(_playerPkKills);
                _player.setCursedWeaponEquippedId(0);
                removeDemonicSkills();

                // Unequip && remove.
                _player.useEquippableItem(_item, true);
                _player.destroyItemByItemId(EItemProcessPurpose.CURSED_WEAPON, _itemId, 1, _player, false);

                _player.broadcastUserInfo();

                _player.store();
            }
            // Player is offline ; make only SQL operations.
            else {
                _log.info(_name + " being removed offline.");

                try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
                    // Delete the item
                    PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
                    statement.setInt(1, _playerId);
                    statement.setInt(2, _itemId);
                    if (statement.executeUpdate() != 1) { _log.warning("Error while deleting itemId " + _itemId + " from userId " + _playerId); }

                    statement.close();

                    // Restore the karma and PK kills.
                    statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE obj_id=?");
                    statement.setInt(1, _playerKarma);
                    statement.setInt(2, _playerPkKills);
                    statement.setInt(3, _playerId);
                    if (statement.executeUpdate() != 1) { _log.warning("Error while updating karma & pkkills for userId " + _playerId); }

                    statement.close();
                }
                catch (Exception e) {
                    _log.log(Level.WARNING, "Could not delete : " + e.getMessage(), e);
                }
            }
        }
        else {
            // This CW is in the inventory of someone who has another cursed weapon equipped.
            if (_player != null && _player.getInventory().getItemByItemId(_itemId) != null) {
                _player.destroyItemByItemId(EItemProcessPurpose.CURSED_WEAPON, _itemId, 1, _player, false);
                _log.info(_name + " item has been assimilated.");
            }
            // This CW is on the ground.
            else if (_item != null) {
                _item.decayMe();
                _log.info(_name + " item has been removed from world.");
            }
        }

        // Drop tasks.
        cancelDailyTimerTask();
        cancelOverallTimerTask();
        cancelDropTimerTask();

        // Delete infos from table, if any.
        removeFromDb();

        // Inform all ppl.
        Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_DISAPPEARED).addItemName(_itemId));

        // Reset state.
        _player = null;
        _item = null;

        _isActivated = false;
        _isDropped = false;

        _nbKills = 0;
        _currentStage = 1;
        _numberBeforeNextStage = 0;

        _hungryTime = 0;
        _endTime = 0;

        _playerId = 0;
        _playerKarma = 0;
        _playerPkKills = 0;
    }

    private void cancelDailyTimerTask() {
        if (_dailyTimerTask != null) {
            _dailyTimerTask.cancel(true);
            _dailyTimerTask = null;
        }
    }

    private void cancelOverallTimerTask() {
        if (_overallTimerTask != null) {
            _overallTimerTask.cancel(true);
            _overallTimerTask = null;
        }
    }

    private void cancelDropTimerTask() {
        if (_dropTimerTask != null) {
            _dropTimerTask.cancel(true);
            _dropTimerTask = null;
        }
    }

    private class DailyTimerTask implements Runnable {
        // Internal timer to delay messages to the next hour, instead of every minute.
        private int _timer = 0;

        protected DailyTimerTask() {
        }

        @Override
        public void run() {
            _hungryTime--;
            _timer++;

            if (_hungryTime <= 0) { endOfLife(); }
            else if (_player != null && _player.isOnline() && _timer % 60 == 0) {
                SystemMessage msg;
                int timeLeft = (int) (getTimeLeft() / 60000);
                if (timeLeft > 60) {
                    msg = SystemMessage.getSystemMessage(SystemMessageId.S2_HOUR_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
                    msg.addItemName(_player.getCursedWeaponEquippedId());
                    msg.addNumber(Math.round(timeLeft / 60));
                }
                else {
                    msg = SystemMessage.getSystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
                    msg.addItemName(_player.getCursedWeaponEquippedId());
                    msg.addNumber(timeLeft);
                }
                _player.sendPacket(msg);
            }
        }
    }

    private class OverallTimerTask implements Runnable {
        protected OverallTimerTask() {
        }

        @Override
        public void run() {
            // Overall timer is reached, ends the life of CW.
            if (System.currentTimeMillis() >= _endTime) { endOfLife(); }
            else
            // Save data.
            { updateData(); }
        }
    }

    private class DropTimerTask implements Runnable {
        protected DropTimerTask() {
        }

        @Override
        public void run() {
            if (isDropped()) { endOfLife(); }
        }
    }

    /**
     * This method is used to drop the CW from player.<br>
     * It drops the item on ground, and reset player stats.
     *
     * @param killer : The player who killed CW owner.
     */
    private void dropFromPlayer(L2Character killer) {
        _player.abortAttack();

        // Prevent item from being removed by ItemsAutoDestroy
        _item.setDestroyProtected(true);
        _player.dropItem(EItemProcessPurpose.DIE_DROP, _item, killer, true);

        _isActivated = false;
        _isDropped = true;

        _player.setKarma(_playerKarma);
        _player.setPkKills(_playerPkKills);
        _player.setCursedWeaponEquippedId(0);
        removeDemonicSkills();

        // Cancel the daily timer. It will be reactivated when someone will pickup the weapon.
        cancelDailyTimerTask();

        // Activate the "1h dropped CW" timer.
        _dropTimerTask = ThreadPoolManager.getInstance().scheduleGeneral(new DropTimerTask(), 3600000L);

        // Reset current stage to 1.
        _currentStage = 1;

        // Drop infos from database.
        removeFromDb();

        SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_WAS_DROPPED_IN_THE_S1_REGION);
        sm.addZoneName(_player.getX(), _player.getY(), _player.getZ());
        sm.addItemName(_itemId);

        Broadcast.toAllOnlinePlayers(sm);
    }

    /**
     * This method is used to drop the CW from a monster.<br>
     * It drops the item on ground, and broadcast earthquake && red sky animations.
     *
     * @param attackable : The monster who dropped CW.
     * @param player     : The player who killed the monster.
     */
    private void dropFromMob(L2Attackable attackable, L2PcInstance player) {
        _isActivated = false;

        // get position
        int x = attackable.getX() + Rnd.get(-70, 70);
        int y = attackable.getY() + Rnd.get(-70, 70);
        int z = GeoData.getInstance().getHeight(x, y, attackable.getZ());

        // create item and drop it
        _item = ItemTable.getInstance().createItem(EItemProcessPurpose.CURSED_WEAPON, _itemId, 1, player, attackable);
        _item.setDestroyProtected(true);
        _item.dropMe(attackable, x, y, z);

        // RedSky and Earthquake
        Broadcast.toAllOnlinePlayers(new ExRedSky(10));
        Broadcast.toAllOnlinePlayers(new Earthquake(x, y, z, 14, 3));

        _isDropped = true;

        SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_WAS_DROPPED_IN_THE_S1_REGION);
        sm.addZoneName(player.getX(), player.getY(), player.getZ());
        sm.addItemName(_itemId);

        Broadcast.toAllOnlinePlayers(sm);
    }

    /**
     * Method used to send messages.<br>
     * <ul>
     * <li>one is broadcasted to warn ppl CW is online.</li>
     * <li>the other shows left timer for for CW owner (either in hours or minutes).</li>
     * </ul>
     */
    public void cursedOnLogin() {
        SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_OWNER_HAS_LOGGED_INTO_THE_S1_REGION);
        msg.addZoneName(_player.getX(), _player.getY(), _player.getZ());
        msg.addItemName(_player.getCursedWeaponEquippedId());
        Broadcast.toAllOnlinePlayers(msg);

        int timeLeft = (int) (getTimeLeft() / 60000);
        if (timeLeft > 60) {
            msg = SystemMessage.getSystemMessage(SystemMessageId.S2_HOUR_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
            msg.addItemName(_player.getCursedWeaponEquippedId());
            msg.addNumber(Math.round(timeLeft / 60));
        }
        else {
            msg = SystemMessage.getSystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
            msg.addItemName(_player.getCursedWeaponEquippedId());
            msg.addNumber(timeLeft);
        }
        _player.sendPacket(msg);
    }

    /**
     * Rebind the passive skill belonging to the CursedWeapon. Invoke this method if the weapon owner switches to a subclass.
     */
    public void giveDemonicSkills() {
        _player.addSkill(SkillTable.getInfo(_skillId, _currentStage), false);
        _player.sendSkillList();
    }

    private void removeDemonicSkills() {
        _player.removeSkill(_skillId);
        _player.sendSkillList();
    }

    /**
     * Reactivate the CW. It can be either coming from a player login, or a GM command.
     *
     * @param fromZero : if set to true, both _hungryTime and _endTime will be reseted to their default values.
     */
    public void reActivate(boolean fromZero) {
        if (fromZero) {
            _hungryTime = _durationLost * 60;
            _endTime = (System.currentTimeMillis() + _duration * 3600000L);

            _overallTimerTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new OverallTimerTask(), 60000L, 60000L);
        }
        else {
            _isActivated = true;
            if (_endTime - System.currentTimeMillis() <= 0) { endOfLife(); }
            else {
                _dailyTimerTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new DailyTimerTask(), 60000L, 60000L);
                _overallTimerTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new OverallTimerTask(), 60000L, 60000L);
            }
        }
    }

    public boolean checkDrop(L2Attackable attackable, L2PcInstance player) {
        if (Rnd.get(1000000) < _dropRate) {
            // Drop the item.
            dropFromMob(attackable, player);

            // Start timers.
            _endTime = System.currentTimeMillis() + _duration * 3600000L;
            _overallTimerTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new OverallTimerTask(), 60000L, 60000L);
            _dropTimerTask = ThreadPoolManager.getInstance().scheduleGeneral(new DropTimerTask(), 3600000L);

            return true;
        }
        return false;
    }

    public void activate(L2PcInstance player, L2ItemInstance item) {
        // if the player is mounted, attempt to unmount first and pick it if successful.
        if (player.isMounted() && !player.dismount()) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(item.getItemId()));
            item.setDestroyProtected(true);
            player.dropItem(EItemProcessPurpose.CURSED_WEAPON, item, null, true);
            return;
        }

        _isActivated = true;

        // Hold player data.
        _player = player;
        _playerId = _player.getObjectId();
        _playerKarma = _player.getKarma();
        _playerPkKills = _player.getPkKills();

        _item = item;

        // Generate a random number for next stage.
        _numberBeforeNextStage = Rnd.get((int) Math.round(_stageKills * 0.5), (int) Math.round(_stageKills * 1.5));

        // Renew hungry time.
        _hungryTime = _durationLost * 60;

        // Activate the daily timer.
        _dailyTimerTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new DailyTimerTask(), 60000L, 60000L);

        // Cancel the "1h dropped CW" timer.
        cancelDropTimerTask();

        insertData();

        // Change player stats
        _player.setCursedWeaponEquippedId(_itemId);
        _player.setKarma(9999999);
        _player.setPkKills(0);

        if (_player.isInParty()) { _player.getParty().removePartyMember(_player, MessageType.Expelled); }

        // Disable active toggles
        for (L2Effect effect : _player.getAllEffects()) {
            if (effect.getSkill().isToggle()) { effect.exit(); }
        }

        // Add CW skills
        giveDemonicSkills();

        // Equip the weapon
        _player.useEquippableItem(_item, true);

        // Fully heal player
        _player.setCurrentHpMp(_player.getMaxHp(), _player.getMaxMp());
        _player.setCurrentCp(_player.getMaxCp());

        // Refresh player stats
        _player.broadcastUserInfo();

        // _player.broadcastPacket(new SocialAction(_player, 17));
        Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION).addZoneName(_player.getX(), _player.getY(), _player.getZ()).addItemName(_item.getItemId()));
    }

    public void loadData() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM cursed_weapons WHERE itemId=?");
            statement.setInt(1, _itemId);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                _playerId = rset.getInt("playerId");
                _playerKarma = rset.getInt("playerKarma");
                _playerPkKills = rset.getInt("playerPkKills");
                _nbKills = rset.getInt("nbKills");
                _currentStage = rset.getInt("currentStage");
                _numberBeforeNextStage = rset.getInt("numberBeforeNextStage");
                _hungryTime = rset.getInt("hungryTime");
                _endTime = rset.getLong("endTime");

                reActivate(false);
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Could not restore CursedWeapons data: " + e.getMessage(), e);
        }
    }

    /**
     * Insert a new line with fresh informations.<br>
     * Use : activate() method.
     */
    private void insertData() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO cursed_weapons (itemId, playerId, playerKarma, playerPkKills, nbKills, currentStage, numberBeforeNextStage, hungryTime, endTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statement.setInt(1, _itemId);
            statement.setInt(2, _playerId);
            statement.setInt(3, _playerKarma);
            statement.setInt(4, _playerPkKills);
            statement.setInt(5, _nbKills);
            statement.setInt(6, _currentStage);
            statement.setInt(7, _numberBeforeNextStage);
            statement.setInt(8, _hungryTime);
            statement.setLong(9, _endTime);
            statement.executeUpdate();
            statement.close();
        }
        catch (SQLException e) {
            _log.log(Level.SEVERE, "CursedWeapon: Failed to insert data.", e);
        }
    }

    /**
     * Update && save dynamic data (a CW must have been already inserted).<br>
     * Use : in the 1min overall task.
     */
    protected void updateData() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE cursed_weapons SET nbKills=?, currentStage=?, numberBeforeNextStage=?, hungryTime=?, endTime=? WHERE itemId=?");
            statement.setInt(1, _nbKills);
            statement.setInt(2, _currentStage);
            statement.setInt(3, _numberBeforeNextStage);
            statement.setInt(4, _hungryTime);
            statement.setLong(5, _endTime);
            statement.setInt(6, _itemId);
            statement.executeUpdate();
            statement.close();
        }
        catch (SQLException e) {
            _log.log(Level.SEVERE, "CursedWeapon: Failed to update data.", e);
        }
    }

    /**
     * Drop dynamic infos regarding CW for the given itemId.<br>
     * Use : in endOfLife() method.
     */
    private void removeFromDb() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            // Delete datas
            PreparedStatement statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
            statement.setInt(1, _itemId);
            statement.executeUpdate();
            statement.close();
        }
        catch (SQLException e) {
            _log.log(Level.SEVERE, "CursedWeapon: Failed to remove data: " + e.getMessage(), e);
        }
    }

    /**
     * This method checks if the CW is dropped or simply dissapears.
     *
     * @param killer : The killer of CW's owner.
     */
    public void dropIt(L2Character killer) {
        // Remove it
        if (Rnd.get(100) <= _disapearChance) { endOfLife(); }
        // Unequip & Drop
        else { dropFromPlayer(killer); }
    }

    /**
     * Increase the number of kills.<br>
     * In case actual counter reaches the number generated to reach next stage, than rank up the CW.
     */
    public void increaseKills() {
        if (_player != null && _player.isOnline()) {
            _nbKills++;
            _hungryTime = _durationLost * 60;

            _player.setPkKills(_player.getPkKills() + 1);
            _player.sendPacket(new UserInfo(_player));

            // If current number of kills is >= to the given number, than rankUp the weapon.
            if (_nbKills >= _numberBeforeNextStage) {
                // Reset the number of kills to 0.
                _nbKills = 0;

                // Setup the new random number.
                _numberBeforeNextStage = Rnd.get((int) Math.round(_stageKills * 0.5), (int) Math.round(_stageKills * 1.5));

                // Rank up the CW.
                rankUp();
            }
        }
    }

    /**
     * This method is used to rank up a CW.
     */
    public void rankUp() {
        if (_currentStage >= _skillMaxLevel) { return; }

        // Rank up current stage.
        _currentStage++;

        // Reward skills for that CW.
        giveDemonicSkills();
    }

    public void setDisapearChance(int disapearChance) {
        _disapearChance = disapearChance;
    }

    public void setDropRate(int dropRate) {
        _dropRate = dropRate;
    }

    public void setDuration(int duration) {
        _duration = duration;
    }

    public void setDurationLost(int durationLost) {
        _durationLost = durationLost;
    }

    public void setStageKills(int stageKills) {
        _stageKills = stageKills;
    }

    public void setPlayer(L2PcInstance player) {
        _player = player;
    }

    public void setItem(L2ItemInstance item) {
        _item = item;
    }

    public boolean isActivated() {
        return _isActivated;
    }

    public boolean isDropped() {
        return _isDropped;
    }

    public long getEndTime() {
        return _endTime;
    }

    public long getDuration() {
        return _duration;
    }

    public int getDurationLost() {
        return _durationLost;
    }

    public String getName() {
        return _name;
    }

    public int getItemId() {
        return _itemId;
    }

    public int getSkillId() {
        return _skillId;
    }

    public int getPlayerId() {
        return _playerId;
    }

    public L2PcInstance getPlayer() {
        return _player;
    }

    public int getPlayerKarma() {
        return _playerKarma;
    }

    public int getPlayerPkKills() {
        return _playerPkKills;
    }

    public int getNbKills() {
        return _nbKills;
    }

    public int getStageKills() {
        return _stageKills;
    }

    public boolean isActive() {
        return _isActivated || _isDropped;
    }

    public long getTimeLeft() {
        return _endTime - System.currentTimeMillis();
    }

    public int getCurrentStage() {
        return _currentStage;
    }

    public int getNumberBeforeNextStage() {
        return _numberBeforeNextStage;
    }

    public int getHungryTime() {
        return _hungryTime;
    }

    public void goTo(L2PcInstance player) {
        if (player == null) { return; }

        // Go to player holding the weapon
        if (_isActivated) { player.teleToLocation(_player.getX(), _player.getY(), _player.getZ(), 0); }
        // Go to item on the ground
        else if (_isDropped) { player.teleToLocation(_item.getX(), _item.getY(), _item.getZ(), 0); }
        else { player.sendMessage(_name + " isn't in the world."); }
    }

    public Location getWorldPosition() {
        if (_isActivated && _player != null) { return _player.getPosition().getWorldPosition(); }

        if (_isDropped && _item != null) { return _item.getPosition().getWorldPosition(); }

        return null;
    }
}
