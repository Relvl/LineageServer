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
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound;
import net.sf.l2j.gameserver.network.client.game_to_client.SocialAction;
import net.sf.l2j.gameserver.network.client.game_to_client.SpecialCamera;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.Util;

public class Valakas extends AbstractNpcAI {
    private static final L2BossZone VALAKAS_LAIR = GrandBossManager.getInstance().getZoneById(110010);

    private static final byte DORMANT = 0; // Valakas is spawned and no one has entered yet. Entry is unlocked.
    private static final byte WAITING = 1; // Valakas is spawned and someone has entered, triggering a 30 minute window for additional people to enter. Entry is unlocked.
    private static final byte FIGHTING = 2; // Valakas is engaged in battle, annihilating his foes. Entry is locked.
    private static final byte DEAD = 3; // Valakas has been killed. Entry is locked.

    private static final int[] FRONT_SKILLS =
            {
                    4681,
                    4682,
                    4683,
                    4684,
                    4689
            };

    private static final int[] BEHIND_SKILLS =
            {
                    4685,
                    4686,
                    4688
            };

    private static final int LAVA_SKIN = 4680;
    private static final int METEOR_SWARM = 4690;

    private static final HeadedLocation[] CUBE_LOC =
            {
                    new HeadedLocation(214880, -116144, -1644, 0),
                    new HeadedLocation(213696, -116592, -1644, 0),
                    new HeadedLocation(212112, -116688, -1644, 0),
                    new HeadedLocation(211184, -115472, -1664, 0),
                    new HeadedLocation(210336, -114592, -1644, 0),
                    new HeadedLocation(211360, -113904, -1644, 0),
                    new HeadedLocation(213152, -112352, -1644, 0),
                    new HeadedLocation(214032, -113232, -1644, 0),
                    new HeadedLocation(214752, -114592, -1644, 0),
                    new HeadedLocation(209824, -115568, -1421, 0),
                    new HeadedLocation(210528, -112192, -1403, 0),
                    new HeadedLocation(213120, -111136, -1408, 0),
                    new HeadedLocation(215184, -111504, -1392, 0),
                    new HeadedLocation(215456, -117328, -1392, 0),
                    new HeadedLocation(213200, -118160, -1424, 0)
            };

    public static final int VALAKAS = 29028;

    private long _timeTracker = 0; // Time tracker for last attack on Valakas.
    private L2Playable _actualVictim; // Actual target of Valakas.

    public Valakas() {
        super("ai/individual");

        int[] mob =
                {
                        VALAKAS
                };
        registerMobs(mob);

        final StatsSet info = GrandBossManager.getInstance().getStatsSet(VALAKAS);
        final int status = GrandBossManager.getInstance().getBossStatus(VALAKAS);

        if (status == DEAD) {
            // load the unlock date and time for valakas from DB
            long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
            if (temp > 0) {
                // The time has not yet expired. Mark Valakas as currently locked (dead).
                startQuestTimer("valakas_unlock", temp, null, null, false);
            }
            else {
                // The time has expired while the server was offline. Spawn valakas in his cave as DORMANT.
                final L2Npc valakas = addSpawn(VALAKAS, -105200, -253104, -15264, 0, false, 0, false);
                GrandBossManager.getInstance().setBossStatus(VALAKAS, DORMANT);
                GrandBossManager.getInstance().addBoss((L2GrandBossInstance) valakas);

                valakas.setIsInvul(true);
                valakas.setRunning();

                valakas.getAI().setIntention(EIntention.IDLE);
            }
        }
        else {
            final int loc_x = info.getInteger("loc_x");
            final int loc_y = info.getInteger("loc_y");
            final int loc_z = info.getInteger("loc_z");
            final int heading = info.getInteger("heading");
            final int hp = info.getInteger("currentHP");
            final int mp = info.getInteger("currentMP");

            final L2Npc valakas = addSpawn(VALAKAS, loc_x, loc_y, loc_z, heading, false, 0, false);
            GrandBossManager.getInstance().addBoss((L2GrandBossInstance) valakas);

            valakas.setCurrentHpMp(hp, mp);
            valakas.setRunning();

            // Start timers.
            if (status == FIGHTING) {
                // stores current time for inactivity task.
                _timeTracker = System.currentTimeMillis();

                startQuestTimer("regen_task", 60000, valakas, null, true);
                startQuestTimer("skill_task", 2000, valakas, null, true);
            }
            else {
                valakas.setIsInvul(true);
                valakas.getAI().setIntention(EIntention.IDLE);

                // Start timer to lock entry after 30 minutes
                if (status == WAITING) { startQuestTimer("beginning", Config.WAIT_TIME_VALAKAS, valakas, null, false); }
            }
        }
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        if (npc != null) {
            if (event.equalsIgnoreCase("beginning")) {
                // Stores current time
                _timeTracker = System.currentTimeMillis();

                // Teleport Valakas to his lair.
                npc.teleToLocation(212852, -114842, -1632, 0);

                // Sound + socialAction.
                for (L2PcInstance plyr : VALAKAS_LAIR.getKnownTypeInside(L2PcInstance.class)) {
                    plyr.sendPacket(new PlaySound(1, "B03_A", 0, 0, 0, 0, 0));
                    plyr.sendPacket(new SocialAction(npc, 3));
                }

                // Launch the cinematic, and tasks (regen + skill).
                startQuestTimer("spawn_1", 1700, npc, null, false); // 1700
                startQuestTimer("spawn_2", 3200, npc, null, false); // 1500
                startQuestTimer("spawn_3", 6500, npc, null, false); // 3300
                startQuestTimer("spawn_4", 9400, npc, null, false); // 2900
                startQuestTimer("spawn_5", 12100, npc, null, false); // 2700
                startQuestTimer("spawn_6", 12430, npc, null, false); // 330
                startQuestTimer("spawn_7", 15430, npc, null, false); // 3000
                startQuestTimer("spawn_8", 16830, npc, null, false); // 1400
                startQuestTimer("spawn_9", 23530, npc, null, false); // 6700 - end of cinematic
                startQuestTimer("spawn_10", 26000, npc, null, false); // 2500 - AI + unlock
            }
            // Regeneration && inactivity task
            else if (event.equalsIgnoreCase("regen_task")) {
                // Inactivity task - 15min
                if (GrandBossManager.getInstance().getBossStatus(VALAKAS) == FIGHTING) {
                    if (_timeTracker + 900000 < System.currentTimeMillis()) {
                        npc.getAI().setIntention(EIntention.IDLE);
                        npc.teleToLocation(-105200, -253104, -15264, 0);

                        GrandBossManager.getInstance().setBossStatus(VALAKAS, DORMANT);
                        npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());

                        // Drop all players from the zone.
                        VALAKAS_LAIR.oustAllPlayers();

                        // Cancel skill_task and regen_task.
                        cancelQuestTimer("regen_task", npc, null);
                        cancelQuestTimer("skill_task", npc, null);
                        return null;
                    }
                }

                // Regeneration buff.
                if (Rnd.get(30) == 0) {
                    L2Skill skillRegen;
                    final double hpRatio = npc.getCurrentHp() / npc.getMaxHp();

                    // Current HPs are inferior to 25% ; apply lvl 4 of regen skill.
                    if (hpRatio < 0.25) { skillRegen = SkillTable.getInfo(4691, 4); }
                    // Current HPs are inferior to 50% ; apply lvl 3 of regen skill.
                    else if (hpRatio < 0.5) { skillRegen = SkillTable.getInfo(4691, 3); }
                    // Current HPs are inferior to 75% ; apply lvl 2 of regen skill.
                    else if (hpRatio < 0.75) { skillRegen = SkillTable.getInfo(4691, 2); }
                    else { skillRegen = SkillTable.getInfo(4691, 1); }

                    skillRegen.getEffects(npc, npc);
                }
            }
            // Spawn cinematic, regen_task and choose of skill.
            else if (event.equalsIgnoreCase("spawn_1")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1800, 180, -1, 1500, 10000, 0, 0, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_2")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1300, 180, -5, 3000, 10000, 0, -5, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_3")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 500, 180, -8, 600, 10000, 0, 60, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_4")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 800, 180, -8, 2700, 10000, 0, 30, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_5")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 200, 250, 70, 0, 10000, 30, 80, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_6")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1100, 250, 70, 2500, 10000, 30, 80, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_7")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 700, 150, 30, 0, 10000, -10, 60, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_8")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1200, 150, 20, 2900, 10000, -10, 30, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_9")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 750, 170, -10, 3400, 4000, 10, -15, 1, 0)); }
            else if (event.equalsIgnoreCase("spawn_10")) {
                GrandBossManager.getInstance().setBossStatus(VALAKAS, FIGHTING);
                npc.setIsInvul(false);

                startQuestTimer("regen_task", 60000, npc, null, true);
                startQuestTimer("skill_task", 2000, npc, null, true);
            }
            // Death cinematic, spawn of Teleport Cubes.
            else if (event.equalsIgnoreCase("die_1")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 2000, 130, -1, 0, 10000, 0, 0, 1, 1)); }
            else if (event.equalsIgnoreCase("die_2")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1100, 210, -5, 3000, 10000, -13, 0, 1, 1)); }
            else if (event.equalsIgnoreCase("die_3")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1300, 200, -8, 3000, 10000, 0, 15, 1, 1)); }
            else if (event.equalsIgnoreCase("die_4")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1000, 190, 0, 500, 10000, 0, 10, 1, 1)); }
            else if (event.equalsIgnoreCase("die_5")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1700, 120, 0, 2500, 10000, 12, 40, 1, 1)); }
            else if (event.equalsIgnoreCase("die_6")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1700, 20, 0, 700, 10000, 10, 10, 1, 1)); }
            else if (event.equalsIgnoreCase("die_7")) { VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1700, 10, 0, 1000, 10000, 20, 70, 1, 1)); }
            else if (event.equalsIgnoreCase("die_8")) {
                VALAKAS_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 1700, 10, 0, 300, 250, 20, -20, 1, 1));

                for (HeadedLocation loc : CUBE_LOC) { addSpawn(31759, loc, false, 900000, false); }

                startQuestTimer("remove_players", 900000, null, null, false);
            }
            else if (event.equalsIgnoreCase("skill_task")) { callSkillAI(npc); }
        }
        else {
            if (event.equalsIgnoreCase("valakas_unlock")) {
                final L2Npc valakas = addSpawn(VALAKAS, -105200, -253104, -15264, 32768, false, 0, false);
                GrandBossManager.getInstance().addBoss((L2GrandBossInstance) valakas);
                GrandBossManager.getInstance().setBossStatus(VALAKAS, DORMANT);
            }
            else if (event.equalsIgnoreCase("remove_players")) { VALAKAS_LAIR.oustAllPlayers(); }
        }
        return super.onAdvEvent(event, npc, player);
    }

    @Override
    public String onSpawn(L2Npc npc) {
        npc.disableCoreAI(true);
        return super.onSpawn(npc);
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) {
        if (!VALAKAS_LAIR.isInsideZone(attacker)) {
            attacker.doDie(attacker);
            return null;
        }

        if (npc.isInvul()) { return null; }

        if (GrandBossManager.getInstance().getBossStatus(VALAKAS) != FIGHTING) {
            attacker.teleToLocation(150037, -57255, -2976, 0);
            return null;
        }

        // Debuff strider-mounted players.
        if (attacker.getMountType() == 1) {
            final L2Skill skill = SkillTable.getInfo(4258, 1);
            if (attacker.getFirstEffect(skill) == null) {
                npc.setTarget(attacker);
                npc.doCast(skill);
            }
        }
        _timeTracker = System.currentTimeMillis();

        return super.onAttack(npc, attacker, damage, isPet);
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet) {
        // Cancel skill_task and regen_task.
        cancelQuestTimer("regen_task", npc, null);
        cancelQuestTimer("skill_task", npc, null);

        // Launch death animation.
        VALAKAS_LAIR.broadcastPacket(new PlaySound(1, "B03_D", 0, 0, 0, 0, 0));

        startQuestTimer("die_1", 300, npc, null, false); // 300
        startQuestTimer("die_2", 600, npc, null, false); // 300
        startQuestTimer("die_3", 3800, npc, null, false); // 3200
        startQuestTimer("die_4", 8200, npc, null, false); // 4400
        startQuestTimer("die_5", 8700, npc, null, false); // 500
        startQuestTimer("die_6", 13300, npc, null, false); // 4600
        startQuestTimer("die_7", 14000, npc, null, false); // 700
        startQuestTimer("die_8", 16500, npc, null, false); // 2500

        GrandBossManager.getInstance().setBossStatus(VALAKAS, DEAD);

        long respawnTime = (long) Config.SPAWN_INTERVAL_VALAKAS + Rnd.get(-Config.RANDOM_SPAWN_TIME_VALAKAS, Config.RANDOM_SPAWN_TIME_VALAKAS);
        respawnTime *= 3600000;

        startQuestTimer("valakas_unlock", respawnTime, null, null, false);

        // also save the respawn time so that the info is maintained past reboots
        StatsSet info = GrandBossManager.getInstance().getStatsSet(VALAKAS);
        info.set("respawn_time", System.currentTimeMillis() + respawnTime);
        GrandBossManager.getInstance().setStatsSet(VALAKAS, info);

        return super.onKill(npc, killer, isPet);
    }

    @Override
    public String onAggro(L2Npc npc, L2PcInstance player, boolean isPet) {
        return null;
    }

    private void callSkillAI(L2Npc npc) {
        if (npc.isInvul() || npc.isCastingNow()) { return; }

        // Pickup a target if no or dead victim. 10% luck he decides to reconsiders his target.
        if (_actualVictim == null || _actualVictim.isDead() || !(npc.getKnownList().knowsObject(_actualVictim)) || Rnd.get(10) == 0) { _actualVictim = getRandomPlayer(npc); }

        // If result is still null, Valakas will roam. Don't go deeper in skill AI.
        if (_actualVictim == null) {
            if (Rnd.get(10) == 0) {
                int x = npc.getX();
                int y = npc.getY();
                int z = npc.getZ();

                int posX = x + Rnd.get(-1400, 1400);
                int posY = y + Rnd.get(-1400, 1400);

                if (PathFinding.getInstance().canMoveToTarget(x, y, z, posX, posY, z)) { npc.getAI().setIntention(EIntention.MOVE_TO, new HeadedLocation(posX, posY, z, 0)); }
            }
            return;
        }

        final L2Skill skill = SkillTable.getInfo(getRandomSkill(npc), 1);

        // Cast the skill or follow the target.
        if (Util.checkIfInRange((skill.getCastRange() < 600) ? 600 : skill.getCastRange(), npc, _actualVictim, true)) {
            npc.getAI().setIntention(EIntention.IDLE);
            npc.setTarget(_actualVictim);
            npc.doCast(skill);
        }
        else { npc.getAI().setIntention(EIntention.FOLLOW, _actualVictim, null); }
    }

    /**
     * Pick a random skill.<br>
     * Valakas will mostly use utility skills. If Valakas feels surrounded, he will use AoE skills.<br>
     * Lower than 50% HPs, he will begin to use Meteor skill.
     *
     * @param npc valakas
     * @return a usable skillId
     */
    private static int getRandomSkill(L2Npc npc) {
        final double hpRatio = npc.getCurrentHp() / npc.getMaxHp();

        // Valakas Lava Skin is prioritary.
        if (hpRatio < 0.25 && Rnd.get(1500) == 0 && npc.getFirstEffect(4680) == null) { return LAVA_SKIN; }

        if (hpRatio < 0.5 && Rnd.get(60) == 0) { return METEOR_SWARM; }

        // Find enemies surrounding Valakas.
        final int[] playersAround = getPlayersCountInPositions(1200, npc, false);

        // Behind position got more ppl than front position, use behind aura skill.
        if (playersAround[1] > playersAround[0]) { return BEHIND_SKILLS[Rnd.get(BEHIND_SKILLS.length)]; }

        // Use front aura skill.
        return FRONT_SKILLS[Rnd.get(FRONT_SKILLS.length)];
    }
}