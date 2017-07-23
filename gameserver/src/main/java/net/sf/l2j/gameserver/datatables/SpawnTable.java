package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SpawnTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnTable.class);

    private final Set<L2Spawn> l2Spawns = ConcurrentHashMap.newKeySet();

    private SpawnTable() { fillSpawnTable(); }

    public static SpawnTable getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Set<L2Spawn> getSpawnTable() {
        return l2Spawns;
    }

    private void fillSpawnTable() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM spawnlist");
             ResultSet rset = statement.executeQuery();
        ) {

            while (rset.next()) {
                NpcTemplate template = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
                if (template != null) {
                    if (template.isType("L2SiegeGuard")) {
                        // Don't spawn guards, they're spawned during castle sieges.
                    }
                    else if (template.isType("L2RaidBoss")) {
                        LOGGER.warn("SpawnTable: RB ({}) is in regular spawnlist, move it in raidboss_spawnlist.", template.getIdTemplate());
                    }
                    else if (!Config.ALLOW_CLASS_MASTERS && template.isType("L2ClassMaster")) {
                        // Dont' spawn class masters (if config is setuped to false).
                    }
                    else if (!Config.WYVERN_ALLOW_UPGRADER && template.isType("L2WyvernManager")) {
                        // Dont' spawn wyvern managers (if config is setuped to false).
                    }
                    else {
                        L2Spawn spawnDat = new L2Spawn(template);
                        spawnDat.setLocx(rset.getInt("locx"));
                        spawnDat.setLocy(rset.getInt("locy"));
                        spawnDat.setLocz(rset.getInt("locz"));
                        spawnDat.setHeading(rset.getInt("heading"));
                        spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
                        spawnDat.setRandomRespawnDelay(rset.getInt("respawn_rand"));

                        switch (rset.getInt("periodOfDay")) {
                            case 0: // default
                                spawnDat.init();
                                break;

                            case 1: // Day
                                DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
                                break;

                            case 2: // Night
                                DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
                                break;
                        }

                        l2Spawns.add(spawnDat);
                    }
                }
                else {
                    LOGGER.warn("SpawnTable: Data missing in NPC table for ID: {}.", rset.getInt("npc_templateid"));
                }
            }
            rset.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("SpawnTable: Spawn could not be initialized: ", e);
        }

        LOGGER.info("SpawnTable: Loaded {} Npc Spawn Locations.", l2Spawns.size());
    }

    public void addNewSpawn(L2Spawn spawn, boolean storeInDb) {
        l2Spawns.add(spawn);

        if (storeInDb) {
            try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement("INSERT INTO spawnlist (npc_templateid,locx,locy,locz,heading,respawn_delay) VALUES(?,?,?,?,?,?)");
                statement.setInt(1, spawn.getNpcId());
                statement.setInt(2, spawn.getLocx());
                statement.setInt(3, spawn.getLocy());
                statement.setInt(4, spawn.getLocz());
                statement.setInt(5, spawn.getHeading());
                statement.setInt(6, spawn.getRespawnDelay() / 1000);
                statement.execute();
                statement.close();
            }
            catch (Exception e) {
                // problem with storing spawn
                LOGGER.error("SpawnTable: Could not store spawn in the DB:", e);
            }
        }
    }

    public void deleteSpawn(L2Spawn spawn, boolean updateDb) {
        if (!l2Spawns.remove(spawn)) { return; }

        if (updateDb) {
            try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement("DELETE FROM spawnlist WHERE locx=? AND locy=? AND locz=? AND npc_templateid=? AND heading=?");
                statement.setInt(1, spawn.getLocx());
                statement.setInt(2, spawn.getLocy());
                statement.setInt(3, spawn.getLocz());
                statement.setInt(4, spawn.getNpcId());
                statement.setInt(5, spawn.getHeading());
                statement.execute();
                statement.close();
            }
            catch (Exception e) {
                // problem with deleting spawn
                LOGGER.error("SpawnTable: Spawn {} could not be removed from DB", spawn, e);
            }
        }
    }

    private static final class SingletonHolder {
        private static final SpawnTable INSTANCE = new SpawnTable();
    }
}