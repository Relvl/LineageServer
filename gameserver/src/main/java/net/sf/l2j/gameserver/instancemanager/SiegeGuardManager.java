package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Castle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SiegeGuardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SiegeGuardManager.class);

    private final Castle _castle;
    private final List<L2Spawn> _siegeGuardSpawn = new ArrayList<>();

    public SiegeGuardManager(Castle castle) {
        _castle = castle;
    }

    /**
     * Add a guard on activeChar's position.
     *
     * @param activeChar The position used.
     * @param npcId      The templte to spawn.
     */
    public void addSiegeGuard(L2PcInstance activeChar, int npcId) {
        if (activeChar == null) { return; }

        addSiegeGuard(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
    }

    /**
     * Add guard following regular coordinates.
     *
     * @param x
     * @param y
     * @param z
     * @param heading
     * @param npcId   The templte to spawn.
     */
    public void addSiegeGuard(int x, int y, int z, int heading, int npcId) {
        saveSiegeGuard(x, y, z, heading, npcId, 0);
    }

    /**
     * Hire merc.
     *
     * @param activeChar
     * @param npcId      The templte to spawn.
     */
    public void hireMerc(L2PcInstance activeChar, int npcId) {
        if (activeChar == null) { return; }

        hireMerc(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
    }

    /**
     * Hire merc.
     *
     * @param x
     * @param y
     * @param z
     * @param heading
     * @param npcId
     */
    public void hireMerc(int x, int y, int z, int heading, int npcId) {
        saveSiegeGuard(x, y, z, heading, npcId, 1);
    }

    /**
     * Remove a single mercenary, identified by the npcId and location. Presumably, this is used when a castle lord picks up a previously dropped ticket
     *
     * @param npcId
     * @param x
     * @param y
     * @param z
     */
    public void removeMerc(int npcId, int x, int y, int z) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM castle_siege_guards WHERE npcId = ? AND x = ? AND y = ? AND z = ? AND isHired = 1");
            statement.setInt(1, npcId);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error deleting hired siege guard at {}" + ',' + "{}" + ',' + "{}: {}", x, y, z, e.getMessage(), e);
        }
    }

    /**
     * Remove mercs.
     */
    public void removeMercs() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM castle_siege_guards WHERE castleId = ? AND isHired = 1");
            statement.setInt(1, _castle.getCastleId());
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error deleting hired siege guard for castle {}: {}", _castle.getName(), e.getMessage(), e);
        }
    }

    /**
     * Spawn guards.
     */
    public void spawnSiegeGuard() {
        try {
            int hiredCount = 0;
            int hiredMax = MercTicketManager.getInstance().getMaxAllowedMerc(_castle.getCastleId());
            boolean isHired = (_castle.getOwnerId() > 0) ? true : false;

            loadSiegeGuard();

            for (L2Spawn spawn : _siegeGuardSpawn) {
                if (spawn != null) {
                    spawn.init();
                    if (isHired) {
                        spawn.stopRespawn();
                        if (++hiredCount > hiredMax) { return; }
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("Error spawning siege guards for castle {}", _castle.getName(), e);
        }
    }

    /**
     * Unspawn guards.
     */
    public void unspawnSiegeGuard() {
        for (L2Spawn spawn : _siegeGuardSpawn) {
            if (spawn == null) { continue; }

            spawn.stopRespawn();
            spawn.getLastSpawn().doDie(spawn.getLastSpawn());
        }

        _siegeGuardSpawn.clear();
    }

    /**
     * Load guards. If castle is owned by a clan, then don't spawn default guards
     */
    private void loadSiegeGuard() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_siege_guards WHERE castleId = ? AND isHired = ?");
            statement.setInt(1, _castle.getCastleId());
            statement.setInt(2, (_castle.getOwnerId() > 0 ? 1 : 0));

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                NpcTemplate template = NpcTable.getInstance().getTemplate(rs.getInt("npcId"));
                if (template != null) {
                    L2Spawn spawn = new L2Spawn(template);
                    spawn.setLocx(rs.getInt("x"));
                    spawn.setLocy(rs.getInt("y"));
                    spawn.setLocz(rs.getInt("z"));
                    spawn.setHeading(rs.getInt("heading"));
                    spawn.setRespawnDelay(rs.getInt("respawnDelay"));

                    _siegeGuardSpawn.add(spawn);
                }
                else { LOGGER.warn("Missing npc data in npc table for id: {}", rs.getInt("npcId")); }
            }
            rs.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error loading siege guard for castle {}: {}", _castle.getName(), e.getMessage(), e);
        }
    }

    /**
     * Save guards.
     *
     * @param x
     * @param y
     * @param z
     * @param heading
     * @param npcId
     * @param isHire
     */
    private void saveSiegeGuard(int x, int y, int z, int heading, int npcId, int isHire) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO castle_siege_guards (castleId, npcId, x, y, z, heading, respawnDelay, isHired) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            statement.setInt(1, _castle.getCastleId());
            statement.setInt(2, npcId);
            statement.setInt(3, x);
            statement.setInt(4, y);
            statement.setInt(5, z);
            statement.setInt(6, heading);
            statement.setInt(7, (isHire == 1 ? 0 : 600));
            statement.setInt(8, isHire);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error adding siege guard for castle {}: {}", _castle.getName(), e.getMessage(), e);
        }
    }

    public final Castle getCastle() {
        return _castle;
    }

    public final List<L2Spawn> getSiegeGuardSpawn() {
        return _siegeGuardSpawn;
    }
}