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
package net.sf.l2j.gameserver.scripting.scripts.ai.individual;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;
import net.sf.l2j.gameserver.templates.StatsSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Core AI
 *
 * @author DrLecter Revised By Emperorc
 */
public class Core extends AbstractNpcAI {
    private static final int CORE = 29006;
    private static final int DEATH_KNIGHT = 29007;
    private static final int DOOM_WRAITH = 29008;
    private static final int SUSCEPTOR = 29011;

    private static final byte ALIVE = 0; // Core is spawned.
    private static final byte DEAD = 1; // Core has been killed.

    private final List<L2Attackable> _minions = new ArrayList<>();

    public Core() {
        super("ai/individual");

        addAttackId(CORE);
        addKillId(CORE, DEATH_KNIGHT, DOOM_WRAITH, SUSCEPTOR);

        StatsSet info = GrandBossManager.getInstance().getStatsSet(CORE);
        int status = GrandBossManager.getInstance().getBossStatus(CORE);
        if (status == DEAD) {
            // load the unlock date and time for Core from DB
            long temp = info.getLong("respawn_time") - System.currentTimeMillis();
            if (temp > 0) {
                // The time has not yet expired. Mark Core as currently locked (dead).
                startQuestTimer("core_unlock", temp, null, null, false);
            }
            else {
                // The time has expired while the server was offline. Spawn Core.
                L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0, false);
                GrandBossManager.getInstance().setBossStatus(CORE, ALIVE);
                spawnBoss(core);
            }
        }
        else {
            int loc_x = info.getInteger("loc_x");
            int loc_y = info.getInteger("loc_y");
            int loc_z = info.getInteger("loc_z");
            int heading = info.getInteger("heading");
            int hp = info.getInteger("currentHP");
            int mp = info.getInteger("currentMP");

            L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, loc_x, loc_y, loc_z, heading, false, 0, false);
            core.setCurrentHpMp(hp, mp);
            spawnBoss(core);
        }
    }

    public void spawnBoss(L2GrandBossInstance npc) {
        GrandBossManager.getInstance().addBoss(npc);
        npc.broadcastPacket(new PlaySound(ESound.BS01_A, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));

        // Spawn minions
        L2Attackable mob;
        for (int i = 0; i < 5; i++) {
            int x = 16800 + i * 360;
            mob = (L2Attackable) addSpawn(DEATH_KNIGHT, x, 110000, npc.getZ(), 280 + Rnd.get(40), false, 0, false);
            mob.setIsRaidMinion(true);
            _minions.add(mob);
            mob = (L2Attackable) addSpawn(DEATH_KNIGHT, x, 109000, npc.getZ(), 280 + Rnd.get(40), false, 0, false);
            mob.setIsRaidMinion(true);
            _minions.add(mob);
            int x2 = 16800 + i * 600;
            mob = (L2Attackable) addSpawn(DOOM_WRAITH, x2, 109300, npc.getZ(), 280 + Rnd.get(40), false, 0, false);
            mob.setIsRaidMinion(true);
            _minions.add(mob);
        }

        for (int i = 0; i < 4; i++) {
            int x = 16800 + i * 450;
            mob = (L2Attackable) addSpawn(SUSCEPTOR, x, 110300, npc.getZ(), 280 + Rnd.get(40), false, 0, false);
            mob.setIsRaidMinion(true);
            _minions.add(mob);
        }
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        if (event.equalsIgnoreCase("core_unlock")) {
            L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0, false);
            GrandBossManager.getInstance().setBossStatus(CORE, ALIVE);
            spawnBoss(core);
        }
        else if (event.equalsIgnoreCase("spawn_minion")) {
            L2Attackable mob = (L2Attackable) addSpawn(npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, false);
            mob.setIsRaidMinion(true);
            _minions.add(mob);
        }
        else if (event.equalsIgnoreCase("despawn_minions")) {
            for (int i = 0; i < _minions.size(); i++) {
                L2Attackable mob = _minions.get(i);
                if (mob != null) { mob.decayMe(); }
            }
            _minions.clear();
        }
        return null;
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) {
        if (npc.isScriptValue(1)) {
            if (Rnd.get(100) == 0) { npc.broadcastNpcSay("Removing intruders."); }
        }
        else {
            npc.setScriptValue(1);
            npc.broadcastNpcSay("A non-permitted target has been discovered.");
            npc.broadcastNpcSay("Starting intruder removal system.");
        }
        return super.onAttack(npc, attacker, damage, isPet);
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet) {
        if (npc.getNpcId() == CORE) {
            npc.broadcastPacket(new PlaySound(ESound.BS02_D, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
            npc.broadcastNpcSay("A fatal error has occurred.");
            npc.broadcastNpcSay("System is being shut down...");
            npc.broadcastNpcSay("......");

            addSpawn(31842, 16502, 110165, -6394, 0, false, 900000, false);
            addSpawn(31842, 18948, 110166, -6397, 0, false, 900000, false);
            GrandBossManager.getInstance().setBossStatus(CORE, DEAD);

            long respawnTime = (long) Config.SPAWN_INTERVAL_CORE + Rnd.get(-Config.RANDOM_SPAWN_TIME_CORE, Config.RANDOM_SPAWN_TIME_CORE);
            respawnTime *= 3600000;

            startQuestTimer("core_unlock", respawnTime, null, null, false);

            StatsSet info = GrandBossManager.getInstance().getStatsSet(CORE);
            info.set("respawn_time", System.currentTimeMillis() + respawnTime);
            GrandBossManager.getInstance().setStatsSet(CORE, info);
            startQuestTimer("despawn_minions", 20000, null, null, false);
            cancelQuestTimers("spawn_minion");
        }
        else if (GrandBossManager.getInstance().getBossStatus(CORE) == ALIVE && _minions != null && _minions.contains(npc)) {
            _minions.remove(npc);
            startQuestTimer("spawn_minion", 60000, npc, null, false);
        }
        return super.onKill(npc, killer, isPet);
    }
}