package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

public class RaidBossSpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaidBossSpawnManager.class);

    protected static final Map<Integer, L2RaidBossInstance> _bosses = new HashMap<>();
    protected static final Map<Integer, L2Spawn> _spawns = new HashMap<>();
    protected static final Map<Integer, StatsSet> _storedInfo = new HashMap<>();
    protected static final Map<Integer, ScheduledFuture<?>> _schedules = new HashMap<>();

    public enum StatusEnum {
        ALIVE,
        DEAD,
        UNDEFINED
    }

    public RaidBossSpawnManager() {
        init();
    }

    public static RaidBossSpawnManager getInstance() {
        return SingletonHolder._instance;
    }

    private void init() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM raidboss_spawnlist ORDER BY boss_id");
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                NpcTemplate template = getValidTemplate(rset.getInt("boss_id"));
                if (template != null) {
                    L2Spawn spawnDat = new L2Spawn(template);
                    spawnDat.setLocx(rset.getInt("loc_x"));
                    spawnDat.setLocy(rset.getInt("loc_y"));
                    spawnDat.setLocz(rset.getInt("loc_z"));
                    spawnDat.setHeading(rset.getInt("heading"));
                    spawnDat.setRespawnMinDelay(rset.getInt("spawn_time"));
                    spawnDat.setRespawnMaxDelay(rset.getInt("random_time"));

                    addNewSpawn(spawnDat, rset.getLong("respawn_time"), rset.getDouble("currentHP"), rset.getDouble("currentMP"), false);
                }
                else {
                    LOGGER.warn("RaidBossSpawnManager: Could not load raidboss #{} from DB", rset.getInt("boss_id"));
                }
            }

            LOGGER.info("RaidBossSpawnManager: Loaded {} instances.", _bosses.size());
            LOGGER.info("RaidBossSpawnManager: Scheduled {} instances.", _schedules.size());

            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("RaidBossSpawnManager: Couldnt load raidboss_spawnlist table.", e);
        }
        catch (Exception e) {
            LOGGER.error("Error while initializing RaidBossSpawnManager: {}", e.getMessage(), e);
        }
    }

    private static class spawnSchedule implements Runnable {
        private final int bossId;

        public spawnSchedule(int npcId) {
            bossId = npcId;
        }

        @Override
        public void run() {
            L2RaidBossInstance raidboss = null;

            if (bossId == 25328) { raidboss = DayNightSpawnManager.getInstance().handleBoss(_spawns.get(bossId)); }
            else { raidboss = (L2RaidBossInstance) _spawns.get(bossId).doSpawn(); }

            if (raidboss != null) {
                raidboss.setRaidStatus(StatusEnum.ALIVE);

                StatsSet info = new StatsSet();
                info.set("currentHP", raidboss.getCurrentHp());
                info.set("currentMP", raidboss.getCurrentMp());
                info.set("respawnTime", 0L);

                _storedInfo.put(bossId, info);

                LOGGER.info("RaidBoss: {} has spawned.", raidboss.getName());

                _bosses.put(bossId, raidboss);
            }

            _schedules.remove(bossId);
        }
    }

    public void updateStatus(L2RaidBossInstance boss, boolean isBossDead) {
        if (!_storedInfo.containsKey(boss.getNpcId())) { return; }

        StatsSet info = _storedInfo.get(boss.getNpcId());

        if (isBossDead) {
            boss.setRaidStatus(StatusEnum.DEAD);

            // getRespawnMinDelay() is used as fixed timer, while getRespawnMaxDelay() is used as random timer.
            int respawnDelay = boss.getSpawn().getRespawnMinDelay() + Rnd.get(-boss.getSpawn().getRespawnMaxDelay(), boss.getSpawn().getRespawnMaxDelay());
            long respawnTime = Calendar.getInstance().getTimeInMillis() + (respawnDelay * 3600000);

            info.set("currentHP", boss.getMaxHp());
            info.set("currentMP", boss.getMaxMp());
            info.set("respawnTime", respawnTime);

            if (!_schedules.containsKey(boss.getNpcId())) {
                LOGGER.info("RaidBoss: {} - {} ({}h).", boss.getName(), new SimpleDateFormat("dd-MM-yyyy HH:mm").format(respawnTime), respawnDelay);

                _schedules.put(boss.getNpcId(), ThreadPoolManager.getInstance().schedule(new spawnSchedule(boss.getNpcId()), respawnDelay * 3600000));
                updateDb();
            }
        }
        else {
            boss.setRaidStatus(StatusEnum.ALIVE);

            info.set("currentHP", boss.getCurrentHp());
            info.set("currentMP", boss.getCurrentMp());
            info.set("respawnTime", 0L);
        }

        _storedInfo.put(boss.getNpcId(), info);
    }

    public void addNewSpawn(L2Spawn spawnDat, long respawnTime, double currentHP, double currentMP, boolean storeInDb) {
        if (spawnDat == null) { return; }

        int bossId = spawnDat.getNpcId();
        if (_spawns.containsKey(bossId)) { return; }

        long time = Calendar.getInstance().getTimeInMillis();

        SpawnTable.getInstance().addNewSpawn(spawnDat, false);

        if (respawnTime == 0L || (time > respawnTime)) {
            L2RaidBossInstance raidboss = null;

            if (bossId == 25328) { raidboss = DayNightSpawnManager.getInstance().handleBoss(spawnDat); }
            else { raidboss = (L2RaidBossInstance) spawnDat.doSpawn(); }

            if (raidboss != null) {
                currentHP = (currentHP == 0) ? raidboss.getMaxHp() : currentHP;
                currentMP = (currentMP == 0) ? raidboss.getMaxMp() : currentMP;

                raidboss.setCurrentHp(currentHP);
                raidboss.setCurrentMp(currentMP);
                raidboss.setRaidStatus(StatusEnum.ALIVE);

                _bosses.put(bossId, raidboss);

                StatsSet info = new StatsSet();
                info.set("currentHP", currentHP);
                info.set("currentMP", currentMP);
                info.set("respawnTime", 0L);

                _storedInfo.put(bossId, info);
            }
        }
        else {
            long spawnTime = respawnTime - Calendar.getInstance().getTimeInMillis();
            _schedules.put(bossId, ThreadPoolManager.getInstance().schedule(new spawnSchedule(bossId), spawnTime));
        }

        _spawns.put(bossId, spawnDat);

        if (storeInDb) {
            try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement("INSERT INTO raidboss_spawnlist (boss_id,loc_x,loc_y,loc_z,heading,respawn_time,currentHp,currentMp) VALUES(?,?,?,?,?,?,?,?)");
                statement.setInt(1, spawnDat.getNpcId());
                statement.setInt(2, spawnDat.getLocx());
                statement.setInt(3, spawnDat.getLocy());
                statement.setInt(4, spawnDat.getLocz());
                statement.setInt(5, spawnDat.getHeading());
                statement.setLong(6, respawnTime);
                statement.setDouble(7, currentHP);
                statement.setDouble(8, currentMP);
                statement.execute();
                statement.close();
            }
            catch (Exception e) {
                // problem with storing spawn
                LOGGER.error("RaidBossSpawnManager: Could not store raidboss #{} in the DB:{}", bossId, e.getMessage(), e);
            }
        }
    }

    public void deleteSpawn(L2Spawn spawnDat, boolean updateDb) {
        if (spawnDat == null) { return; }

        int bossId = spawnDat.getNpcId();
        if (!_spawns.containsKey(bossId)) { return; }

        SpawnTable.getInstance().deleteSpawn(spawnDat, false);
        _spawns.remove(bossId);

        if (_bosses.containsKey(bossId)) { _bosses.remove(bossId); }

        if (_schedules.containsKey(bossId)) {
            ScheduledFuture<?> f = _schedules.remove(bossId);
            f.cancel(true);
        }

        if (_storedInfo.containsKey(bossId)) { _storedInfo.remove(bossId); }

        if (updateDb) {
            try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement("DELETE FROM raidboss_spawnlist WHERE boss_id=?");
                statement.setInt(1, bossId);
                statement.execute();
                statement.close();
            }
            catch (Exception e) {
                // problem with deleting spawn
                LOGGER.error("RaidBossSpawnManager: Could not remove raidboss #{} from DB: {}", bossId, e.getMessage(), e);
            }
        }
    }

    private void updateDb() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE raidboss_spawnlist SET respawn_time = ?, currentHP = ?, currentMP = ? WHERE boss_id = ?");

            for (Entry<Integer, StatsSet> infoEntry : _storedInfo.entrySet()) {
                int bossId = infoEntry.getKey();

                L2RaidBossInstance boss = _bosses.get(bossId);
                if (boss == null) { continue; }

                if (boss.getRaidStatus().equals(StatusEnum.ALIVE)) { updateStatus(boss, false); }

                StatsSet info = infoEntry.getValue();
                if (info == null) { continue; }

                statement.setLong(1, info.getLong("respawnTime"));
                statement.setDouble(2, info.getDouble("currentHP"));
                statement.setDouble(3, info.getDouble("currentMP"));
                statement.setInt(4, bossId);
                statement.executeUpdate();
                statement.clearParameters();
            }
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("RaidBossSpawnManager: Couldnt update raidboss_spawnlist table {}", e.getMessage(), e);
        }
    }

    public StatusEnum getRaidBossStatusId(int bossId) {
        if (_bosses.containsKey(bossId)) { return _bosses.get(bossId).getRaidStatus(); }

        if (_schedules.containsKey(bossId)) { return StatusEnum.DEAD; }

        return StatusEnum.UNDEFINED;
    }

    public NpcTemplate getValidTemplate(int bossId) {
        NpcTemplate template = NpcTable.getInstance().getTemplate(bossId);
        if (template == null) { return null; }

        if (!template.isType("L2RaidBoss")) { return null; }

        return template;
    }

    public void notifySpawnNightBoss(L2RaidBossInstance raidboss) {
        StatsSet info = new StatsSet();
        info.set("currentHP", raidboss.getCurrentHp());
        info.set("currentMP", raidboss.getCurrentMp());
        info.set("respawnTime", 0L);

        raidboss.setRaidStatus(StatusEnum.ALIVE);

        _storedInfo.put(raidboss.getNpcId(), info);
        _bosses.put(raidboss.getNpcId(), raidboss);

        LOGGER.info("RaidBossSpawnManager: Spawning Night Raid Boss {}", raidboss.getName());
    }

    public boolean isDefined(int bossId) {
        return _spawns.containsKey(bossId);
    }

    public Map<Integer, L2RaidBossInstance> getBosses() {
        return _bosses;
    }

    public Map<Integer, L2Spawn> getSpawns() {
        return _spawns;
    }

    public void reloadBosses() {
        init();
    }

    /**
     * Saves all raidboss status and then clears all info from memory, including all schedules.
     */
    public void cleanUp() {
        updateDb();

        _bosses.clear();

        if (!_schedules.isEmpty()) {
            for (ScheduledFuture<?> f : _schedules.values()) { f.cancel(true); }

            _schedules.clear();
        }

        _storedInfo.clear();
        _spawns.clear();
    }

    private static class SingletonHolder {
        protected static final RaidBossSpawnManager _instance = new RaidBossSpawnManager();
    }
}