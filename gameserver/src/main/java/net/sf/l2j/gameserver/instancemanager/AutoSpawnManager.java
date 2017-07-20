package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.util.Broadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoSpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSpawnManager.class);

    private static final int DEFAULT_INITIAL_SPAWN = 30000; // 30 seconds after registration
    private static final int DEFAULT_RESPAWN = 3600000; // 1 hour in millisecs
    private static final int DEFAULT_DESPAWN = 3600000; // 1 hour in millisecs

    protected Map<Integer, AutoSpawnInstance> _registeredSpawns;
    protected Map<Integer, ScheduledFuture<?>> _runningSpawns;

    protected boolean _activeState = true;

    protected AutoSpawnManager() {
        _registeredSpawns = new ConcurrentHashMap<>();
        _runningSpawns = new ConcurrentHashMap<>();

        restoreSpawnData();
    }

    public static AutoSpawnManager getInstance() {
        return SingletonHolder._instance;
    }

    public final int size() {
        return _registeredSpawns.size();
    }

    private void restoreSpawnData() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            // Restore spawn group data, then the location data.
            PreparedStatement statement = con.prepareStatement("SELECT * FROM random_spawn ORDER BY groupId ASC");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                // Register random spawn group, set various options on the
                // created spawn instance.
                AutoSpawnInstance spawnInst = registerSpawn(rs.getInt("npcId"), rs.getInt("initialDelay"), rs.getInt("respawnDelay"), rs.getInt("despawnDelay"));

                spawnInst.setSpawnCount(rs.getInt("count"));
                spawnInst.setBroadcast(rs.getBoolean("broadcastSpawn"));
                spawnInst.setRandomSpawn(rs.getBoolean("randomSpawn"));
                // Restore the spawn locations for this spawn group/instance.
                PreparedStatement statement2 = con.prepareStatement("SELECT * FROM random_spawn_loc WHERE groupId=?");
                statement2.setInt(1, rs.getInt("groupId"));
                ResultSet rs2 = statement2.executeQuery();

                while (rs2.next()) {
                    // Add each location to the spawn group/instance.
                    spawnInst.addSpawnLocation(rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"), rs2.getInt("heading"));
                }
                rs2.close();
                statement2.close();
            }
            rs.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("AutoSpawnManager: Could not restore spawn data", e);
        }
    }

    /**
     * Registers a spawn with the given parameters with the spawner, and marks it as active.<br>
     * Returns a AutoSpawnInstance containing info about the spawn.
     *
     * @param npcId        npc id
     * @param spawnPoints  count
     * @param initialDelay : (If < 0 = default value)
     * @param respawnDelay : (If < 0 = default value)
     * @param despawnDelay : despawnDelay (If < 0 = default value or if = 0, function disabled)
     * @return AutoSpawnInstance spawnInst
     */
    public AutoSpawnInstance registerSpawn(int npcId, int[][] spawnPoints, int initialDelay, int respawnDelay, int despawnDelay) {
        if (initialDelay < 0) { initialDelay = DEFAULT_INITIAL_SPAWN; }

        if (respawnDelay < 0) { respawnDelay = DEFAULT_RESPAWN; }

        if (despawnDelay < 0) { despawnDelay = DEFAULT_DESPAWN; }

        AutoSpawnInstance newSpawn = new AutoSpawnInstance(npcId, initialDelay, respawnDelay, despawnDelay);

        if (spawnPoints != null) { for (int[] spawnPoint : spawnPoints) { newSpawn.addSpawnLocation(spawnPoint); } }

        int newId = IdFactory.getInstance().getNextId();
        newSpawn._objectId = newId;
        _registeredSpawns.put(newId, newSpawn);

        setSpawnActive(newSpawn, true);

        return newSpawn;
    }

    /**
     * Registers a spawn with the given parameters with the spawner, and marks it as active.<BR>
     * Returns a AutoSpawnInstance containing info about the spawn.<BR>
     * <BR>
     * <B>Warning:</B> Spawn locations must be specified separately using addSpawnLocation().
     *
     * @param npcId        npc id
     * @param initialDelay (If < 0 = default value)
     * @param respawnDelay (If < 0 = default value)
     * @param despawnDelay (If < 0 = default value or if = 0, function disabled)
     * @return AutoSpawnInstance spawnInst
     */
    public AutoSpawnInstance registerSpawn(int npcId, int initialDelay, int respawnDelay, int despawnDelay) {
        return registerSpawn(npcId, null, initialDelay, respawnDelay, despawnDelay);
    }

    public boolean removeSpawn(AutoSpawnInstance spawnInst) {
        if (!isSpawnRegistered(spawnInst)) { return false; }

        try {
            // Try to remove from the list of registered spawns if it exists.
            _registeredSpawns.remove(spawnInst.getObjectId());

            // Cancel the currently associated running scheduled task.
            ScheduledFuture<?> respawnTask = _runningSpawns.remove(spawnInst._objectId);
            respawnTask.cancel(false);
        }
        catch (Exception e) {
            LOGGER.error("AutoSpawnManager: Could not auto spawn for NPC ID {} (Object ID = {})", spawnInst._npcId, spawnInst._objectId, e);
            return false;
        }

        return true;
    }

    public void setSpawnActive(AutoSpawnInstance spawnInst, boolean isActive) {
        if (spawnInst == null) { return; }

        int objectId = spawnInst._objectId;

        if (isSpawnRegistered(objectId)) {
            ScheduledFuture<?> spawnTask = null;

            if (isActive) {
                AutoSpawner rs = new AutoSpawner(objectId);

                if (spawnInst._desDelay > 0) { spawnTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(rs, spawnInst._initDelay, spawnInst._resDelay); }
                else { spawnTask = ThreadPoolManager.getInstance().scheduleEffect(rs, spawnInst._initDelay); }

                _runningSpawns.put(objectId, spawnTask);
            }
            else {
                AutoDespawner rd = new AutoDespawner(objectId);
                spawnTask = _runningSpawns.remove(objectId);

                if (spawnTask != null) { spawnTask.cancel(false); }

                ThreadPoolManager.getInstance().scheduleEffect(rd, 0);
            }

            spawnInst.setSpawnActive(isActive);
        }
    }

    public void setAllActive(boolean isActive) {
        if (_activeState == isActive) { return; }

        for (AutoSpawnInstance spawnInst : _registeredSpawns.values()) { setSpawnActive(spawnInst, isActive); }

        _activeState = isActive;
    }

    public final long getTimeToNextSpawn(AutoSpawnInstance spawnInst) {
        int objectId = spawnInst.getObjectId();

        if (!isSpawnRegistered(objectId)) { return -1; }

        return _runningSpawns.get(objectId).getDelay(TimeUnit.MILLISECONDS);
    }

    public final AutoSpawnInstance getAutoSpawnInstance(int id, boolean isObjectId) {
        if (isObjectId) {
            if (isSpawnRegistered(id)) { return _registeredSpawns.get(id); }
        }
        else {
            for (AutoSpawnInstance spawnInst : _registeredSpawns.values()) { if (spawnInst.getNpcId() == id) { return spawnInst; } }
        }
        return null;
    }

    public Map<Integer, AutoSpawnInstance> getAutoSpawnInstances(int npcId) {
        Map<Integer, AutoSpawnInstance> spawnInstList = new HashMap<>();

        for (AutoSpawnInstance spawnInst : _registeredSpawns.values()) { if (spawnInst.getNpcId() == npcId) { spawnInstList.put(spawnInst.getObjectId(), spawnInst); } }

        return spawnInstList;
    }

    public final boolean isSpawnRegistered(int objectId) {
        return _registeredSpawns.containsKey(objectId);
    }

    public final boolean isSpawnRegistered(AutoSpawnInstance spawnInst) {
        return _registeredSpawns.containsValue(spawnInst);
    }

    private static class SingletonHolder {
        protected static final AutoSpawnManager _instance = new AutoSpawnManager();
    }

    private class AutoSpawner implements Runnable {
        private final int _objectId;

        protected AutoSpawner(int objectId) {
            _objectId = objectId;
        }

        @Override
        public void run() {
            try {
                // Retrieve the required spawn instance for this spawn task.
                AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);

                // If the spawn is not scheduled to be active, cancel the spawn task.
                if (!spawnInst.isSpawnActive()) { return; }

                HeadedLocation[] locationList = spawnInst.getLocationList();

                // If there are no set co-ordinates, cancel the spawn task.
                if (locationList.length == 0) {
                    LOGGER.info("AutoSpawnManager: No location co-ords specified for spawn instance (Object ID = {}).", _objectId);
                    return;
                }

                int locationCount = locationList.length;
                int locationIndex = Rnd.get(locationCount);

                // If random spawning is disabled, the spawn at the next set of co-ordinates after the last. If the index is greater than the number of possible spawns, reset the counter to zero.
                if (!spawnInst.isRandomSpawn()) {
                    locationIndex = spawnInst._lastLocIndex;
                    locationIndex++;

                    if (locationIndex == locationCount) { locationIndex = 0; }

                    spawnInst._lastLocIndex = locationIndex;
                }

                // Set the X, Y and Z co-ordinates, where this spawn will take place.
                final int x = locationList[locationIndex].getX();
                final int y = locationList[locationIndex].getY();
                final int z = locationList[locationIndex].getZ();
                final int heading = locationList[locationIndex].getHeading();

                // Fetch the template for this NPC ID and create a new spawn.
                NpcTemplate npcTemp = NpcTable.getInstance().getTemplate(spawnInst.getNpcId());
                if (npcTemp == null) {
                    LOGGER.warn("Couldnt find npcId: {}.", spawnInst.getNpcId());
                    return;
                }
                L2Spawn newSpawn = new L2Spawn(npcTemp);

                newSpawn.setLocx(x);
                newSpawn.setLocy(y);
                newSpawn.setLocz(z);

                if (heading != -1) { newSpawn.setHeading(heading); }

                if (spawnInst._desDelay == 0) { newSpawn.setRespawnDelay(spawnInst._resDelay); }

                // Add the new spawn information to the spawn table, but do not
                // store it.
                SpawnTable.getInstance().addNewSpawn(newSpawn, false);
                L2Npc npcInst = null;

                if (spawnInst._spawnCount == 1) {
                    npcInst = newSpawn.doSpawn();
                    npcInst.getPosition().setXYZ(npcInst.getX(), npcInst.getY(), npcInst.getZ());
                    spawnInst.addNpcInstance(npcInst);
                }
                else {
                    for (int i = 0; i < spawnInst._spawnCount; i++) {
                        npcInst = newSpawn.doSpawn();

                        // To prevent spawning of more than one NPC in the exact same spot, move it slightly by a small random offset.
                        npcInst.getPosition().setXYZ(npcInst.getX() + Rnd.get(50), npcInst.getY() + Rnd.get(50), npcInst.getZ());

                        // Add the NPC instance to the list of managed instances.
                        spawnInst.addNpcInstance(npcInst);
                    }
                }

                // Announce to all players that the spawn has taken place, with the nearest town location.
                if (npcInst != null && spawnInst.isBroadcasting()) {
                    Broadcast.announceToOnlinePlayers("The " + npcInst.getName() + " has spawned near " + MapRegionTable.getInstance().getClosestTownName(npcInst.getX(), npcInst.getY()) + "!");
                }

                // If there is no despawn time, do not create a despawn task.
                if (spawnInst.getDespawnDelay() > 0) { ThreadPoolManager.getInstance().scheduleAi(new AutoDespawner(_objectId), spawnInst.getDespawnDelay() - 1000); }
            }
            catch (Exception e) {
                LOGGER.error("AutoSpawnManager: An error occurred while initializing spawn instance (Object ID = {}): {}", _objectId, e.getMessage(), e);
            }
        }
    }

    /**
     * AutoDespawner Class <BR>
     * <BR>
     * Simply used as a secondary class for despawning an auto spawn instance.
     *
     * @author Tempy
     */
    private class AutoDespawner implements Runnable {
        private final int _objectId;

        protected AutoDespawner(int objectId) {
            _objectId = objectId;
        }

        @Override
        public void run() {
            try {
                AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);

                if (spawnInst == null) {
                    LOGGER.info("AutoSpawnManager: No spawn registered for object ID = {}.", _objectId);
                    return;
                }

                for (L2Npc npcInst : spawnInst.getNPCInstanceList()) {
                    if (npcInst == null) { continue; }

                    npcInst.deleteMe();
                    spawnInst.removeNpcInstance(npcInst);
                }
            }
            catch (Exception e) {
                LOGGER.error("AutoSpawnManager: An error occurred while despawning spawn (Object ID = {})", _objectId, e);
            }
        }
    }

    /**
     * AutoSpawnInstance Class <BR>
     * <BR>
     * Stores information about a registered auto spawn.
     *
     * @author Tempy
     */
    public class AutoSpawnInstance {
        private final List<L2Npc> _npcList = new ArrayList<>();
        private final List<HeadedLocation> _locList = new ArrayList<>();
        protected int _objectId;
        protected int _npcId;
        protected int _initDelay;
        protected int _resDelay;
        protected int _desDelay;
        protected int _spawnCount = 1;
        protected int _lastLocIndex = -1;
        private boolean _spawnActive;

        private boolean _randomSpawn = false;

        private boolean _broadcastAnnouncement = false;

        protected AutoSpawnInstance(int npcId, int initDelay, int respawnDelay, int despawnDelay) {
            _npcId = npcId;
            _initDelay = initDelay;
            _resDelay = respawnDelay;
            _desDelay = despawnDelay;
        }

        protected boolean addNpcInstance(L2Npc npcInst) {
            return _npcList.add(npcInst);
        }

        protected boolean removeNpcInstance(L2Npc npcInst) {
            return _npcList.remove(npcInst);
        }

        public int getObjectId() {
            return _objectId;
        }

        public int getInitialDelay() {
            return _initDelay;
        }

        public int getRespawnDelay() {
            return _resDelay;
        }

        public int getDespawnDelay() {
            return _desDelay;
        }

        public int getNpcId() {
            return _npcId;
        }

        public int getSpawnCount() {
            return _spawnCount;
        }

        public void setSpawnCount(int spawnCount) {
            _spawnCount = spawnCount;
        }

        public HeadedLocation[] getLocationList() {
            return _locList.toArray(new HeadedLocation[_locList.size()]);
        }

        public L2Npc[] getNPCInstanceList() {
            L2Npc[] ret;
            synchronized (_npcList) {
                ret = new L2Npc[_npcList.size()];
                _npcList.toArray(ret);
            }

            return ret;
        }

        public L2Spawn[] getSpawns() {
            List<L2Spawn> npcSpawns = new ArrayList<>();

            for (L2Npc npcInst : _npcList) { npcSpawns.add(npcInst.getSpawn()); }

            return npcSpawns.toArray(new L2Spawn[npcSpawns.size()]);
        }

        public void setBroadcast(boolean broadcastValue) {
            _broadcastAnnouncement = broadcastValue;
        }

        public boolean isSpawnActive() {
            return _spawnActive;
        }

        protected void setSpawnActive(boolean activeValue) {
            _spawnActive = activeValue;
        }

        public boolean isRandomSpawn() {
            return _randomSpawn;
        }

        public void setRandomSpawn(boolean randValue) {
            _randomSpawn = randValue;
        }

        public boolean isBroadcasting() {
            return _broadcastAnnouncement;
        }

        public boolean addSpawnLocation(int x, int y, int z, int heading) {
            return _locList.add(new HeadedLocation(x, y, z, heading));
        }

        public boolean addSpawnLocation(int[] spawnLoc) {
            if (spawnLoc.length != 3) { return false; }

            return addSpawnLocation(spawnLoc[0], spawnLoc[1], spawnLoc[2], -1);
        }
    }
}