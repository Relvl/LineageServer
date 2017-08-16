package net.sf.l2j.gameserver.model;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MinionList {
    protected final L2MonsterInstance _master;
    private final List<L2MonsterInstance> _minionReferences;

    public MinionList(L2MonsterInstance master) {
        _master = master;
        _minionReferences = new CopyOnWriteArrayList<>();
    }

    /**
     * Init a Minion and add it in the world as a visible object.
     * <ul>
     * <li>Get the template of the Minion to spawn</li>
     * <li>Create and Init the Minion and generate its Identifier</li>
     * <li>Set the Minion HP, MP and Heading</li>
     * <li>Set the Minion leader to this RaidBoss</li>
     * <li>Init the position of the Minion and add it in the world as a visible object</li><BR>
     * </ul>
     *
     * @param master   L2MonsterInstance used as master for this minion
     * @param minionId The L2NpcTemplate Identifier of the Minion to spawn
     * @return the instance of the new minion.
     */
    public static final L2MonsterInstance spawnMinion(L2MonsterInstance master, int minionId) {
        // Get the template of the Minion to spawn
        NpcTemplate minionTemplate = NpcTable.getInstance().getTemplate(minionId);
        if (minionTemplate == null) { return null; }

        // Create and Init the Minion and generate its Identifier
        L2MonsterInstance minion = new L2MonsterInstance(IdFactory.getInstance().getNextId(), minionTemplate);
        return initializeNpcInstance(master, minion);
    }

    protected static final L2MonsterInstance initializeNpcInstance(L2MonsterInstance master, L2MonsterInstance minion) {
        minion.stopAllEffects();
        minion.setIsDead(false);
        minion.setDecayed(false);

        // Set the Minion HP, MP and Heading
        minion.setCurrentHpMp(minion.getMaxHp(), minion.getMaxMp());
        minion.setHeading(master.getHeading());

        // Set the Minion leader to this RaidBoss
        minion.setLeader(master);

        // Init the position of the Minion and add it in the world as a visible object
        int offset = 100 + minion.getCollisionRadius() + master.getCollisionRadius();
        int minRadius = master.getCollisionRadius() + 30;

        int newX = Rnd.get(minRadius * 2, offset * 2); // x
        int newY = Rnd.get(newX, offset * 2); // distance
        newY = (int) Math.sqrt(newY * newY - newX * newX); // y
        if (newX > offset + minRadius) { newX = master.getX() + newX - offset; }
        else { newX = master.getX() - newX + minRadius; }
        if (newY > offset + minRadius) { newY = master.getY() + newY - offset; }
        else { newY = master.getY() - newY + minRadius; }

        minion.spawnMe(newX, newY, master.getZ());

        return minion;
    }

    /**
     * @return list of the spawned (alive) minions.
     */
    public List<L2MonsterInstance> getSpawnedMinions() {
        return _minionReferences;
    }

    // hooks

    /**
     * Manage the spawn of Minions.
     * <ul>
     * <li>Get the Minion data of all Minions that must be spawn</li>
     * <li>For each Minion type, spawn the amount of Minion needed</li>
     * </ul>
     */
    public final void spawnMinions() {
        if (_master.isAlikeDead()) { return; }

        List<L2MinionData> minions = _master.getTemplate().getMinionData();
        if (minions == null) { return; }

        int minionCount, minionId, minionsToSpawn;
        for (L2MinionData minion : minions) {
            minionCount = minion.getAmount();
            minionId = minion.getMinionId();

            minionsToSpawn = minionCount - countSpawnedMinionsById(minionId);
            if (minionsToSpawn > 0) {
                for (int i = 0; i < minionsToSpawn; i++) { spawnMinion(_master, minionId); }
            }
        }
    }

    /**
     * Delete all spawned minions and try to reuse them.
     */
    public void deleteSpawnedMinions() {
        if (!_minionReferences.isEmpty()) {
            for (L2MonsterInstance minion : _minionReferences) {
                if (minion != null) {
                    minion.setLeader(null);
                    minion.deleteMe();
                }
            }
            _minionReferences.clear();
        }
    }

    /**
     * Called on the minion spawn and added them in the list of the spawned minions.
     *
     * @param minion The instance of minion.
     */
    public void onMinionSpawn(L2MonsterInstance minion) {
        _minionReferences.add(minion);
    }

    /**
     * Called on the master death/delete.
     *
     * @param force if true - force delete of the spawned minions By default minions deleted only for raidbosses
     */
    public void onMasterDie(boolean force) {
        if (_master.isRaid() || force) { deleteSpawnedMinions(); }
    }

    /**
     * Called on the minion death/delete. Removed minion from the list of the spawned minions and reuse if possible.
     *
     * @param minion      The minion to make checks on.
     * @param respawnTime (ms) enable respawning of this minion while master is alive. -1 - use default value: 0 (disable) for mobs and config value for raids.
     */
    public void onMinionDie(L2MonsterInstance minion, int respawnTime) {
        minion.setLeader(null); // prevent memory leaks
        _minionReferences.remove(minion);

        int time = _master.isRaid() ? (int) Config.RAID_MINION_RESPAWN_TIMER : respawnTime;
        if (time > 0 && !_master.isAlikeDead()) { ThreadPoolManager.getInstance().schedule(new MinionRespawnTask(minion), time); }
    }

    /**
     * Called if master/minion was attacked. Master and all free minions receive aggro against attacker.
     *
     * @param caller   That instance will call for help versus attacker.
     * @param attacker That instance will receive all aggro.
     */
    public void onAssist(L2Character caller, L2Character attacker) {
        if (attacker == null) { return; }

        if (!_master.isAlikeDead() && !_master.isInCombat()) { _master.addDamageHate(attacker, 0, 1); }

        boolean callerIsMaster = caller == _master;
        int aggro = callerIsMaster ? 10 : 1;
        if (_master.isRaid()) { aggro *= 10; }

        for (L2MonsterInstance minion : _minionReferences) {
            if (minion != null && !minion.isDead() && (callerIsMaster || !minion.isInCombat())) { minion.addDamageHate(attacker, 0, aggro); }
        }
    }

    /**
     * Called from onTeleported() of the master Alive and able to move minions teleported to master.
     */
    public void onMasterTeleported() {
        int offset = 200;
        int minRadius = _master.getCollisionRadius() + 30;

        for (L2MonsterInstance minion : _minionReferences) {
            if (minion != null && !minion.isDead() && !minion.isMovementDisabled()) {
                int newX = Rnd.get(minRadius * 2, offset * 2); // x
                int newY = Rnd.get(newX, offset * 2); // distance
                newY = (int) Math.sqrt(newY * newY - newX * newX); // y
                if (newX > offset + minRadius) { newX = _master.getX() + newX - offset; }
                else { newX = _master.getX() - newX + minRadius; }
                if (newY > offset + minRadius) { newY = _master.getY() + newY - offset; }
                else { newY = _master.getY() - newY + minRadius; }

                minion.teleToLocation(newX, newY, _master.getZ(), 0);
            }
        }
    }

    private final int countSpawnedMinionsById(int minionId) {
        int count = 0;
        for (L2MonsterInstance minion : _minionReferences) {
            if (minion != null && minion.getNpcId() == minionId) { count++; }
        }
        return count;
    }

    private final class MinionRespawnTask implements Runnable {
        private final L2MonsterInstance _minion;

        private MinionRespawnTask(L2MonsterInstance minion) {
            _minion = minion;
        }

        @Override
        public void run() {
            if (!_master.isAlikeDead() && _master.isVisible()) {
                // minion can be already spawned or deleted
                if (!_minion.isVisible()) {
                    _minion.refreshID();
                    initializeNpcInstance(_master, _minion);
                }
            }
        }
    }
}