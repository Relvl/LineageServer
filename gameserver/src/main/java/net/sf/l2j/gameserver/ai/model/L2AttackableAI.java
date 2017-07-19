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
package net.sf.l2j.gameserver.ai.model;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.instance.*;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate.AIType;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.Util;

import java.util.List;
import java.util.concurrent.Future;

public class L2AttackableAI extends L2CharacterAI implements Runnable {
    protected static final int RANDOM_WALK_RATE = 30;
    protected static final int MAX_ATTACK_TIMEOUT = 120000; // 2min

    /** The L2Attackable AI task executed every 1s (call onEvtThink method) */
    protected Future<?> _aiTask;

    /** The delay after wich the attacked is stopped */
    protected long _attackTimeout;

    /** The L2Attackable aggro counter */
    protected int _globalAggro;

    /** The flag used to indicate that a thinking action is in progress ; prevent recursive thinking */
    protected boolean _thinking;

    private int _chaostime = 0;

    public L2AttackableAI(L2Attackable attackable) {
        super(attackable);

        _attackTimeout = Long.MAX_VALUE;
        _globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
    }

    @Override
    public void run() {
        // Launch actions corresponding to the Event Think
        onEvtThink();
    }

    /**
     * <B><U> Actor is a L2GuardInstance</U> :</B>
     * <ul>
     * <li>The target isn't a Folk or a Door</li>
     * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
     * <li>The target is in the actor Aggro range and is at the same height</li>
     * <li>The L2PcInstance target has karma (=PK)</li>
     * <li>The L2MonsterInstance target is aggressive</li>
     * </ul>
     * <B><U> Actor is a L2SiegeGuardInstance</U> :</B>
     * <ul>
     * <li>The target isn't a Folk or a Door</li>
     * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
     * <li>The target is in the actor Aggro range and is at the same height</li>
     * <li>A siege is in progress</li>
     * <li>The L2PcInstance target isn't a Defender</li>
     * </ul>
     * <B><U> Actor is a L2FriendlyMobInstance</U> :</B>
     * <ul>
     * <li>The target isn't a Folk, a Door or another L2Npc</li>
     * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
     * <li>The target is in the actor Aggro range and is at the same height</li>
     * <li>The L2PcInstance target has karma (=PK)</li>
     * </ul>
     * <B><U> Actor is a L2MonsterInstance</U> :</B>
     * <ul>
     * <li>The target isn't a Folk, a Door or another L2Npc</li>
     * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
     * <li>The target is in the actor Aggro range and is at the same height</li>
     * <li>The actor is Aggressive</li>
     * </ul>
     *
     * @param target The targeted L2Object
     * @return True if the target is autoattackable (depends on the actor type).
     */
    protected boolean autoAttackCondition(L2Character target) {
        // Check if the target isn't null, a Door or dead.
        if (target == null || target instanceof L2DoorInstance || target.isAlikeDead()) { return false; }

        final L2Attackable me = getActiveChar();

        if (target instanceof L2Playable) {
            // Check if target is in the Aggro range
            if (!me.isInsideRadius(target, me.getAggroRange(), true, false)) { return false; }

            // Check if the AI isn't a Raid Boss, can See Silent Moving players and the target isn't in silent move mode
            if (!(me.isRaid()) && !(me.canSeeThroughSilentMove()) && ((L2Playable) target).isSilentMoving()) { return false; }

            // Check if the target is a L2PcInstance
            L2PcInstance targetPlayer = target.getActingPlayer();
            if (targetPlayer != null) {
                // GM checks ; check if the target is invisible or got access level
                if (targetPlayer.isGM() && (targetPlayer.getAppearance().getInvisible() || !targetPlayer.getAccessLevel().canTakeAggro())) { return false; }

                // Check if player is an allied Varka.
                if (Util.contains(me.getClans(), "varka_silenos_clan") && targetPlayer.isAlliedWithVarka()) { return false; }

                // Check if player is an allied Ketra.
                if (Util.contains(me.getClans(), "ketra_orc_clan") && targetPlayer.isAlliedWithKetra()) { return false; }

                // check if the target is within the grace period for JUST getting up from fake death
                if (targetPlayer.isRecentFakeDeath()) { return false; }

                if (targetPlayer.isInParty() && targetPlayer.getParty().isInDimensionalRift()) {
                    byte riftType = targetPlayer.getParty().getDimensionalRift().getType();
                    byte riftRoom = targetPlayer.getParty().getDimensionalRift().getCurrentRoom();

                    if (me instanceof L2RiftInvaderInstance && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ())) { return false; }
                }
            }
        }

        // Check if the actor is a L2GuardInstance
        if (me instanceof L2GuardInstance) {
            // Check if the L2PcInstance target has karma (=PK)
            if (target.isPlayer() && ((L2PcInstance) target).getKarma() > 0) { return PathFinding.getInstance().canSeeTarget(me, target); }

            // Check if the L2MonsterInstance target is aggressive
            if (target instanceof L2MonsterInstance && Config.GUARD_ATTACK_AGGRO_MOB) { return (((L2MonsterInstance) target).isAggressive() && PathFinding.getInstance().canSeeTarget(me, target)); }

            return false;
        }
        // The actor is a L2FriendlyMobInstance
        else if (me instanceof L2FriendlyMobInstance) {
            // Check if the target isn't another L2Npc
            if (target instanceof L2Npc) { return false; }

            // Check if the L2PcInstance target has karma (=PK)
            if (target.isPlayer() && ((L2PcInstance) target).getKarma() > 0) {
                return PathFinding.getInstance().canSeeTarget(me, target); // Los Check
            }

            return false;
        }
        // The actor is a L2Npc
        else {
            if (target instanceof L2Attackable && me.isConfused()) { return PathFinding.getInstance().canSeeTarget(me, target); }

            if (target instanceof L2Npc) { return false; }

            // depending on config, do not allow mobs to attack _new_ players in peacezones,
            // unless they are already following those players from outside the peacezone.
            if (!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE)) { return false; }

            // Check if the actor is Aggressive
            return (me.isAggressive() && PathFinding.getInstance().canSeeTarget(me, target));
        }
    }

    @Override
    public void stopAITask() {
        if (_aiTask != null) {
            _aiTask.cancel(false);
            _aiTask = null;
        }
        super.stopAITask();
    }

    /**
     * Set the Intention of this L2CharacterAI and create an AI Task executed every 1s (call onEvtThink method) for this L2Attackable.<BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : If actor _knowPlayer isn't EMPTY, IDLE will be change in ACTIVE</B></FONT><BR>
     * <BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0      The first parameter of the Intention
     * @param arg1      The second parameter of the Intention
     */
    @Override
    protected synchronized void changeIntention(EIntention intention, Object arg0, Object arg1) {
        if (intention == EIntention.IDLE || intention == EIntention.ACTIVE) {
            // Check if actor is not dead
            L2Attackable npc = getActiveChar();
            if (!npc.isAlikeDead()) {
                // If its _knownPlayer isn't empty set the Intention to ACTIVE
                if (!npc.getKnownList().getKnownType(L2PcInstance.class).isEmpty()) { intention = EIntention.ACTIVE; }
                else {
                    if (npc.getSpawn() != null) {
                        final int range = Config.MAX_DRIFT_RANGE;
                        if (!npc.isInsideRadius(npc.getSpawn().getLocx(), npc.getSpawn().getLocy(), npc.getSpawn().getLocz(), range + range, true, false)) { intention = EIntention.ACTIVE; }
                    }
                }
            }

            if (intention == EIntention.IDLE) {
                // Set the Intention of this L2AttackableAI to IDLE
                super.changeIntention(EIntention.IDLE, null, null);

                // Stop AI task and detach AI from NPC
                if (_aiTask != null) {
                    _aiTask.cancel(true);
                    _aiTask = null;
                }

                // Cancel the AI
                actor.detachAI();
                return;
            }
        }

        // Set the Intention of this L2AttackableAI to intention
        super.changeIntention(intention, arg0, arg1);

        // If not idle - create an AI task (schedule onEvtThink repeatedly)
        if (_aiTask == null) { _aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000); }
    }

    /**
     * Manage the Attack Intention :
     * <ul>
     * <li>Stop current Attack (if necessary).</li>
     * <li>Calculate attack timeout.</li>
     * <li>Start a new Attack and Launch Think Event.</li>
     * </ul>
     *
     * @param target The L2Character to attack
     */
    @Override
    protected void onIntentionAttack(L2Character target) {
        // Calculate the attack timeout
        _attackTimeout = System.currentTimeMillis() + MAX_ATTACK_TIMEOUT;

        // Buffs
        if (Rnd.get(RANDOM_WALK_RATE) == 0) {
            for (L2Skill sk : getActiveChar().getTemplate().getBuffSkills()) {
                if (getActiveChar().getFirstEffect(sk) != null) { continue; }

                clientStopMoving(null);
                actor.doCast(sk);
            }
        }

        // Manage the attack intention : stop current attack (if necessary), start a new attack and launch Think event.
        super.onIntentionAttack(target);
    }

    private void thinkCast() {
        if (checkTargetLost(getTarget())) {
            setTarget(null);
            return;
        }

        if (maybeMoveToPawn(getTarget(), currentlyCastingSkill.getCastRange())) { return; }

        clientStopMoving(null);
        setIntention(EIntention.ACTIVE);
        actor.doCast(currentlyCastingSkill);
    }

    /**
     * Manage AI standard thinks of a L2Attackable (called by onEvtThink).
     * <ul>
     * <li>Update every 1s the _globalAggro counter to come close to 0</li>
     * <li>If the actor is Aggressive and can attack, add all autoAttackable L2Character in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
     * <li>If the actor is a L2GuardInstance that can't attack, order to it to return to its home location</li>
     * <li>If the actor is a L2MonsterInstance that can't attack, order to it to random walk (1/100)</li>
     * </ul>
     */
    protected void thinkActive() {
        final L2Attackable npc = getActiveChar();

        // Update every 1s the _globalAggro counter to come close to 0
        if (_globalAggro != 0) {
            if (_globalAggro < 0) { _globalAggro++; }
            else { _globalAggro--; }
        }

        // Add all autoAttackable L2Character in L2Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
        // A L2Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
        if (_globalAggro >= 0) {
            // Get all visible objects inside its Aggro Range
            for (L2Character target : npc.getKnownList().getKnownType(L2Character.class)) {
                // Check to see if this is a festival mob spawn. If it is, then check to see if the aggro trigger is a festival participant...if so, move to attack it.
                if (npc instanceof L2FestivalMonsterInstance && target.isPlayer()) {
                    if (!((L2PcInstance) target).isFestivalParticipant()) { continue; }
                }

                // For each L2Character check if the target is autoattackable
                if (autoAttackCondition(target)) // check aggression
                {
                    // Add the attacker to the L2Attackable _aggroList
                    if (npc.getHating(target) == 0) { npc.addDamageHate(target, 0, 0); }
                }
            }

            if (!npc.isCoreAIDisabled()) {
                // Chose a target from its aggroList and order to attack the target
                final L2Character hated = (L2Character) ((npc.isConfused()) ? getTarget() : npc.getMostHated());
                if (hated != null) {
                    // Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
                    if (npc.getHating(hated) + _globalAggro > 0) {
                        // Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
                        npc.setRunning();

                        // Set the AI Intention to ATTACK
                        setIntention(EIntention.ATTACK, hated);
                    }
                    return;
                }
            }
        }

        // Order to the L2GuardInstance to return to its home location because there's no target to attack
        if (npc instanceof L2GuardInstance) {
            /**
             * TODO returnHome for guards currently don't launch setIsReturningToSpawnPoint flag.<br>
             * Either drop that flag entirely through experiences with geodata, put it on for that particular case, or create a new system (based on timer and teleport if failed to reach the place in X time).
             */
            ((L2GuardInstance) npc).returnHome();
            return;
        }

        // If this is a festival monster, then it remains in the same location.
        if (npc instanceof L2FestivalMonsterInstance) { return; }

        // Minions following leader
        final L2Attackable leader = npc.getLeader();
        if (leader != null && !leader.isAlikeDead()) {
            final int offset = 100 + npc.getCollisionRadius() + leader.getCollisionRadius();
            final int minRadius = leader.getCollisionRadius() + 30;

            if (leader.isRunning()) { npc.setRunning(); }
            else { npc.setWalking(); }

            if (npc.getPlanDistanceSq(leader.getX(), leader.getY()) > offset * offset) {
                int x1 = Rnd.get(minRadius * 2, offset * 2); // x
                int y1 = Rnd.get(x1, offset * 2); // distance

                y1 = (int) Math.sqrt(y1 * y1 - x1 * x1); // y

                if (x1 > offset + minRadius) { x1 = leader.getX() + x1 - offset; }
                else { x1 = leader.getX() - x1 + minRadius; }

                if (y1 > offset + minRadius) { y1 = leader.getY() + y1 - offset; }
                else { y1 = leader.getY() - y1 + minRadius; }

                // Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
                moveTo(x1, y1, leader.getZ());
                return;
            }
            else if (Rnd.get(RANDOM_WALK_RATE) == 0) {
                for (L2Skill sk : getActiveChar().getTemplate().getBuffSkills()) {
                    if (npc.getFirstEffect(sk) != null) { continue; }

                    clientStopMoving(null);
                    actor.doCast(sk);
                    return;
                }
            }
        }
        // Order to the L2MonsterInstance to random walk
        else if (npc.getSpawn() != null && Rnd.get(RANDOM_WALK_RATE) == 0 && !npc.isNoRndWalk()) {
            for (L2Skill sk : getActiveChar().getTemplate().getBuffSkills()) {
                if (npc.getFirstEffect(sk) != null) { continue; }

                clientStopMoving(null);
                actor.doCast(sk);
                return;
            }

            int x1 = npc.getSpawn().getLocx();
            int y1 = npc.getSpawn().getLocy();
            int z1 = npc.getSpawn().getLocz();

            final int range = Config.MAX_DRIFT_RANGE;

            if (!npc.isInsideRadius(x1, y1, range, false)) { npc.setIsReturningToSpawnPoint(true); }
            else {
                x1 = Rnd.get(range * 2); // x
                y1 = Rnd.get(x1, range * 2); // distance
                y1 = (int) Math.sqrt(y1 * y1 - x1 * x1); // y
                x1 += npc.getSpawn().getLocx() - range;
                y1 += npc.getSpawn().getLocy() - range;
                z1 = npc.getZ();
            }

            // Move the actor to Location (x,y,z)
            moveTo(x1, y1, z1);
        }
    }

    /**
     * Manage AI attack thoughts of a L2Attackable (called by onEvtThink).
     * <ul>
     * <li>Update the attack timeout if actor is running.</li>
     * <li>If target is dead or timeout is expired, stop this attack and set the Intention to ACTIVE.</li>
     * <li>Call all L2Object of its Faction inside the Faction Range.</li>
     * <li>Choose a target and order to attack it with magic skill or physical attack.</li>
     * </ul>
     */
    protected void thinkAttack() {
        final L2Attackable npc = getActiveChar();
        if (npc.isCastingNow()) { return; }

        L2Character attackTarget = (L2Character) getTarget();

        // If target doesn't exist, is dead or if timeout is expired (non-aggro mobs or mobs which are too far stop to attack)
        if (attackTarget == null || attackTarget.isAlikeDead() || (_attackTimeout < System.currentTimeMillis() && (!npc.isAggressive() || Math.sqrt(npc.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY())) > npc.getAggroRange() * 2))) {
            // Stop hating this target after the attack timeout or if target is dead
            if (attackTarget != null) { npc.stopHating(attackTarget); }

            // Search the nearest target. If a target is found, continue regular process, else drop angry behavior.
            attackTarget = targetReconsider(npc.getAggroRange(), false);
            if (attackTarget == null) {
                setIntention(EIntention.ACTIVE);
                npc.setWalking();
                return;
            }
        }

        final int actorCollision = npc.getTemplate().getCollisionRadius();

        /**
         * FACTION CHECKS<br>
         * Handle agression behavior towards monsters of same faction.
         */

        final String[] actorClans = npc.getClans();
        if (actorClans != null) {
            for (L2Npc called : npc.getKnownList().getKnownTypeInRadius(L2Npc.class, npc.getClanRange())) {
                // Caller clan doesn't correspond to the called clan.
                if (!Util.contains(actorClans, called.getClans())) { continue; }

                // Called mob doesnt care about that type of caller id (the bitch !).
                if (Util.contains(called.getIgnoredIds(), npc.getNpcId())) { continue; }

                // Check if the L2Object is inside the Faction Range of the actor
                if (called.hasAI() && PathFinding.getInstance().canSeeTarget(npc, called) && npc.getAttackByList().contains(attackTarget) && (called.getAI().intention == EIntention.IDLE || called.getAI().intention == EIntention.ACTIVE)) {
                    if (attackTarget instanceof L2Playable) {
                        List<Quest> quests = called.getTemplate().getEventQuests(EventType.ON_FACTION_CALL);
                        if (quests != null) {
                            L2PcInstance player = attackTarget.getActingPlayer();
                            boolean isSummon = attackTarget instanceof L2Summon;
                            for (Quest quest : quests) { quest.notifyFactionCall(called, npc, player, isSummon); }
                        }
                    }
                    else if (called instanceof L2Attackable && called.getAI().intention != EIntention.ATTACK) {
                        ((L2Attackable) called).addDamageHate(attackTarget, 0, npc.getHating(attackTarget));
                        called.getAI().setIntention(EIntention.ATTACK, attackTarget);
                    }
                }
            }
        }

        // Corpse AIs, as AI scripts, are stopped here.
        if (npc.isCoreAIDisabled()) { return; }

        /**
         * TARGET CHECKS<br>
         * Chaos time for RB/minions.
         */

        if (npc.isRaid() || npc.isRaidMinion()) {
            _chaostime++;
            if (npc instanceof L2RaidBossInstance && _chaostime > Config.RAID_CHAOS_TIME && (Rnd.get(100) <= 100 - (npc.getCurrentHp() * ((((L2MonsterInstance) npc).hasMinions()) ? 200 : 100) / npc.getMaxHp()))) {
                attackTarget = aggroReconsider(attackTarget);
                _chaostime = 0;
            }
            else if (npc instanceof L2GrandBossInstance && _chaostime > Config.GRAND_CHAOS_TIME) {
                double chaosRate = 100 - (npc.getCurrentHp() * 300 / npc.getMaxHp());
                if ((chaosRate <= 10 && Rnd.get(100) <= 10) || (chaosRate > 10 && Rnd.get(100) <= chaosRate)) {
                    attackTarget = aggroReconsider(attackTarget);
                    _chaostime = 0;
                }
            }
            else if (_chaostime > Config.MINION_CHAOS_TIME && (Rnd.get(100) <= 100 - (npc.getCurrentHp() * 200 / npc.getMaxHp()))) {
                attackTarget = aggroReconsider(attackTarget);
                _chaostime = 0;
            }
        }

        setTarget(attackTarget);
        npc.setTarget(attackTarget);

        /**
         * COMMON INFORMATIONS<br>
         * Used for range and distance check.
         */

        final int combinedCollision = actorCollision + attackTarget.getTemplate().getCollisionRadius();
        final double dist = Math.sqrt(npc.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY()));

        int range = combinedCollision;
        if (attackTarget.isMoving()) { range += 15; }

        if (npc.isMoving()) { range += 15; }

        /**
         * CAST CHECK<br>
         * The mob succeeds a skill check ; make all possible checks to define the skill to launch. If nothing is found, go in MELEE CHECK.<br>
         * It will check skills arrays in that order :
         * <ul>
         * <li>suicide skill at 15% max HPs</li>
         * <li>buff skill if such effect isn't existing</li>
         * <li>heal skill if self or ally is under 75% HPs (priority to others healers and mages)</li>
         * <li>debuff skill if such effect isn't existing</li>
         * <li>damage skill, in that order : short range and long range</li>
         * </ul>
         */

        if (willCastASpell()) {
            // This list is used in order to avoid multiple calls on skills lists. Tests are made one after the other, and content is replaced when needed.
            List<L2Skill> defaultList;

            // -------------------------------------------------------------------------------
            // Suicide possibility if HPs are < 15%.
            defaultList = getActiveChar().getTemplate().getSuicideSkills();
            if (!defaultList.isEmpty() && (npc.getCurrentHp() / npc.getMaxHp() < 0.15)) {
                final L2Skill skill = Rnd.get(defaultList);
                if (cast(skill, dist, range + skill.getSkillRadius())) { return; }
            }

            // -------------------------------------------------------------------------------
            // Heal
            defaultList = getActiveChar().getTemplate().getHealSkills();
            if (!defaultList.isEmpty()) {
                // First priority is to heal leader (if npc is a minion).
                if (npc.isMinion()) {
                    L2Character leader = npc.getLeader();
                    if (leader != null && !leader.isDead() && (leader.getCurrentHp() / leader.getMaxHp() < 0.75)) {
                        for (L2Skill sk : defaultList) {
                            if (sk.getTargetType() == ESkillTargetType.TARGET_SELF) { continue; }

                            if (!checkSkillCastConditions(sk)) { continue; }

                            final int overallRange = sk.getCastRange() + actorCollision + leader.getTemplate().getCollisionRadius();
                            if (!Util.checkIfInRange(overallRange, npc, leader, false) && sk.getTargetType() != ESkillTargetType.TARGET_PARTY && !npc.isMovementDisabled()) {
                                moveToPawn(leader, overallRange);
                                return;
                            }

                            if (PathFinding.getInstance().canSeeTarget(npc, leader)) {
                                clientStopMoving(null);
                                npc.setTarget(leader);
                                npc.doCast(sk);
                                return;
                            }
                        }
                    }
                }

                // Second priority is to heal himself.
                if (npc.getCurrentHp() / npc.getMaxHp() < 0.75) {
                    for (L2Skill sk : defaultList) {
                        if (!checkSkillCastConditions(sk)) { continue; }

                        clientStopMoving(null);
                        npc.setTarget(npc);
                        npc.doCast(sk);
                        return;
                    }
                }

                for (L2Skill sk : defaultList) {
                    if (!checkSkillCastConditions(sk)) { continue; }

                    if (sk.getTargetType() == ESkillTargetType.TARGET_ONE) {
                        for (L2Attackable obj : npc.getKnownList().getKnownTypeInRadius(L2Attackable.class, sk.getCastRange() + actorCollision)) {
                            if (obj.isDead()) { continue; }

                            if (!Util.contains(actorClans, obj.getClans())) { continue; }

                            if (obj.getCurrentHp() / obj.getMaxHp() < 0.75) {
                                if (PathFinding.getInstance().canSeeTarget(npc, obj)) {
                                    clientStopMoving(null);
                                    npc.setTarget(obj);
                                    npc.doCast(sk);
                                    return;
                                }
                            }
                        }

                        if (sk.getTargetType() == ESkillTargetType.TARGET_PARTY) {
                            clientStopMoving(null);
                            npc.doCast(sk);
                            return;
                        }
                    }
                }
            }

            // -------------------------------------------------------------------------------
            // Debuff - 10% luck to get debuffed.
            defaultList = getActiveChar().getTemplate().getDebuffSkills();
            if (Rnd.get(100) < 10 && !defaultList.isEmpty()) {
                for (L2Skill sk : defaultList) {
                    if (!checkSkillCastConditions(sk) || (sk.getCastRange() + npc.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius() <= dist && !canAura(sk))) { continue; }

                    if (!PathFinding.getInstance().canSeeTarget(npc, attackTarget)) { continue; }

                    if (attackTarget.getFirstEffect(sk) == null) {
                        clientStopMoving(null);
                        npc.doCast(sk);
                        return;
                    }
                }
            }

            // -------------------------------------------------------------------------------
            // General attack skill - short range is checked, then long range.
            defaultList = getActiveChar().getTemplate().getShortRangeSkills();
            if (!defaultList.isEmpty() && dist <= 150) {
                final L2Skill skill = Rnd.get(defaultList);
                if (cast(skill, dist, skill.getCastRange())) { return; }
            }
            else {
                defaultList = getActiveChar().getTemplate().getLongRangeSkills();
                if (!defaultList.isEmpty() && dist > 150) {
                    final L2Skill skill = Rnd.get(defaultList);
                    if (cast(skill, dist, skill.getCastRange())) { return; }
                }
            }
        }

        /**
         * MELEE CHECK<br>
         * The mob failed a skill check ; make him flee if AI authorizes it, else melee attack.
         */

        // The range takes now in consideration physical attack range.
        range += npc.getPhysicalAttackRange();

        if (npc.isMovementDisabled()) {
            // If distance is too big, choose another target.
            if (dist > range) { attackTarget = targetReconsider(range, true); }

            // Any AI type, even healer or mage, will try to melee attack if it can't do anything else (desesperate situation).
            if (attackTarget != null) { actor.doAttack(attackTarget); }

            return;
        }

        /**
         * MOVE AROUND CHECK<br>
         * In case many mobs are trying to hit from same place, move a bit, circling around the target
         */

        if (Rnd.get(100) <= 3) {
            for (L2Object nearby : npc.getKnownList().getKnownObjects()) {
                if (nearby instanceof L2Attackable && npc.isInsideRadius(nearby, actorCollision, false, false) && nearby != attackTarget) {
                    int newX = combinedCollision + Rnd.get(40);
                    if (Rnd.nextBoolean()) { newX = attackTarget.getX() + newX; }
                    else { newX = attackTarget.getX() - newX; }

                    int newY = combinedCollision + Rnd.get(40);
                    if (Rnd.nextBoolean()) { newY = attackTarget.getY() + newY; }
                    else { newY = attackTarget.getY() - newY; }

                    if (!npc.isInsideRadius(newX, newY, actorCollision, false)) {
                        int newZ = npc.getZ() + 30;
                        if (PathFinding.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ)) { moveTo(newX, newY, newZ); }
                    }
                    return;
                }
            }
        }

        /**
         * FLEE CHECK<br>
         * Test the flee possibility. Archers got 25% chance to flee.
         */

        if (npc.getAiType() == AIType.ARCHER && dist <= (60 + combinedCollision) && Rnd.get(4) > 1) {
            final int posX = npc.getX() + ((attackTarget.getX() < npc.getX()) ? 300 : -300);
            final int posY = npc.getY() + ((attackTarget.getY() < npc.getY()) ? 300 : -300);
            final int posZ = npc.getZ() + 30;

            if (PathFinding.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ)) {
                setIntention(EIntention.MOVE_TO, new HeadedLocation(posX, posY, posZ, 0));
                return;
            }
        }

        /**
         * BASIC MELEE ATTACK
         */

        if (maybeMoveToPawn(getTarget(), npc.getPhysicalAttackRange())) { return; }

        clientStopMoving(null);
        actor.doAttack((L2Character) getTarget());
    }

    protected boolean cast(L2Skill sk, double distance, int range) {
        if (sk == null) { return false; }

        final L2Attackable caster = getActiveChar();

        if (caster.isCastingNow() && !sk.isSimultaneousCast()) { return false; }

        if (!checkSkillCastConditions(sk)) { return false; }

        if (getTarget() == null) { if (caster.getMostHated() != null) { setTarget(caster.getMostHated()); } }

        L2Character attackTarget = (L2Character) getTarget();
        if (attackTarget == null) { return false; }

        switch (sk.getSkillType()) {
            case BUFF: {
                if (caster.getFirstEffect(sk) == null) {
                    clientStopMoving(null);
                    caster.setTarget(caster);
                    caster.doCast(sk);
                    return true;
                }

                // ----------------------------------------
                // If actor already have buff, start looking at others same faction mob to cast
                if (sk.getTargetType() == ESkillTargetType.TARGET_SELF) { return false; }

                if (sk.getTargetType() == ESkillTargetType.TARGET_ONE) {
                    L2Character target = targetReconsider(sk.getCastRange(), true);
                    if (target != null) {
                        clientStopMoving(null);
                        L2Object targets = attackTarget;
                        caster.setTarget(target);
                        caster.doCast(sk);
                        caster.setTarget(targets);
                        return true;
                    }
                }

                if (canParty(sk)) {
                    clientStopMoving(null);
                    L2Object targets = attackTarget;
                    caster.setTarget(caster);
                    caster.doCast(sk);
                    caster.setTarget(targets);
                    return true;
                }
                break;
            }

            case HEAL:
            case HOT:
            case HEAL_PERCENT:
            case HEAL_STATIC:
            case BALANCE_LIFE: {
                // Minion case.
                if (caster.isMinion() && sk.getTargetType() != ESkillTargetType.TARGET_SELF) {
                    L2Character leader = caster.getLeader();
                    if (leader != null && !leader.isDead() && Rnd.get(100) > (leader.getCurrentHp() / leader.getMaxHp() * 100)) {
                        final int overallRange = sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius();
                        if (!Util.checkIfInRange(overallRange, caster, leader, false) && sk.getTargetType() != ESkillTargetType.TARGET_PARTY && !caster.isMovementDisabled()) { moveToPawn(leader, overallRange); }

                        if (PathFinding.getInstance().canSeeTarget(caster, leader)) {
                            clientStopMoving(null);
                            caster.setTarget(leader);
                            caster.doCast(sk);
                            return true;
                        }
                    }
                }

                // Personal case.
                double percentage = caster.getCurrentHp() / caster.getMaxHp() * 100;
                if (Rnd.get(100) < (100 - percentage) / 3) {
                    clientStopMoving(null);
                    caster.setTarget(caster);
                    caster.doCast(sk);
                    return true;
                }

                if (sk.getTargetType() == ESkillTargetType.TARGET_ONE) {
                    for (L2Attackable obj : caster.getKnownList().getKnownTypeInRadius(L2Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius())) {
                        if (obj.isDead()) { continue; }

                        if (!Util.contains(caster.getClans(), obj.getClans())) { continue; }

                        percentage = obj.getCurrentHp() / obj.getMaxHp() * 100;
                        if (Rnd.get(100) < (100 - percentage) / 10) {
                            if (PathFinding.getInstance().canSeeTarget(caster, obj)) {
                                clientStopMoving(null);
                                caster.setTarget(obj);
                                caster.doCast(sk);
                                return true;
                            }
                        }
                    }
                }

                if (sk.getTargetType() == ESkillTargetType.TARGET_PARTY) {
                    for (L2Attackable obj : caster.getKnownList().getKnownTypeInRadius(L2Attackable.class, sk.getSkillRadius() + caster.getTemplate().getCollisionRadius())) {
                        if (!Util.contains(caster.getClans(), obj.getClans())) { continue; }

                        if (obj.getCurrentHp() < obj.getMaxHp() && Rnd.get(100) <= 20) {
                            clientStopMoving(null);
                            caster.setTarget(caster);
                            caster.doCast(sk);
                            return true;
                        }
                    }
                }
                break;
            }

            case DEBUFF:
            case POISON:
            case DOT:
            case MDOT:
            case BLEED: {
                if (PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && distance <= range) {
                    if (attackTarget.getFirstEffect(sk) == null) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                else if (canAOE(sk)) {
                    if (sk.getTargetType() == ESkillTargetType.TARGET_AURA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AURA) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }

                    if ((sk.getTargetType() == ESkillTargetType.TARGET_AREA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AREA) && PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && distance <= range) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                else if (sk.getTargetType() == ESkillTargetType.TARGET_ONE) {
                    L2Character target = targetReconsider(sk.getCastRange(), true);
                    if (target != null) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                break;
            }

            case SLEEP: {
                if (sk.getTargetType() == ESkillTargetType.TARGET_ONE) {
                    if (!attackTarget.isDead() && distance <= range) {
                        if (distance > range || attackTarget.isMoving()) {
                            if (attackTarget.getFirstEffect(sk) == null) {
                                clientStopMoving(null);
                                caster.doCast(sk);
                                return true;
                            }
                        }
                    }

                    L2Character target = targetReconsider(sk.getCastRange(), true);
                    if (target != null) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                else if (canAOE(sk)) {
                    if (sk.getTargetType() == ESkillTargetType.TARGET_AURA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AURA) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }

                    if ((sk.getTargetType() == ESkillTargetType.TARGET_AREA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AREA) && PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && distance <= range) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                break;
            }

            case ROOT:
            case STUN:
            case PARALYZE: {
                if (PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && distance <= range) {
                    if (attackTarget.getFirstEffect(sk) == null) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                else if (canAOE(sk)) {
                    if (sk.getTargetType() == ESkillTargetType.TARGET_AURA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AURA) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                    else if ((sk.getTargetType() == ESkillTargetType.TARGET_AREA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AREA) && PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && distance <= range) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                else if (sk.getTargetType() == ESkillTargetType.TARGET_ONE) {
                    L2Character target = targetReconsider(sk.getCastRange(), true);
                    if (target != null) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                break;
            }

            case MUTE:
            case FEAR: {
                if (PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && distance <= range) {
                    if (attackTarget.getFirstEffect(sk) == null) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                else if (canAOE(sk)) {
                    if (sk.getTargetType() == ESkillTargetType.TARGET_AURA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AURA) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }

                    if ((sk.getTargetType() == ESkillTargetType.TARGET_AREA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AREA) && PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && distance <= range) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                else if (sk.getTargetType() == ESkillTargetType.TARGET_ONE) {
                    L2Character target = targetReconsider(sk.getCastRange(), true);
                    if (target != null) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                break;
            }

            case CANCEL:
            case NEGATE: {
                // decrease cancel probability
                if (Rnd.get(50) != 0) { return true; }

                if (sk.getTargetType() == ESkillTargetType.TARGET_ONE) {
                    if (attackTarget.getFirstEffect(L2EffectType.BUFF) != null && PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && distance <= range) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }

                    L2Character target = targetReconsider(sk.getCastRange(), true);
                    if (target != null) {
                        clientStopMoving(null);
                        L2Object targets = attackTarget;
                        caster.setTarget(target);
                        caster.doCast(sk);
                        caster.setTarget(targets);
                        return true;
                    }
                }
                else if (canAOE(sk)) {
                    if ((sk.getTargetType() == ESkillTargetType.TARGET_AURA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AURA) && PathFinding.getInstance().canSeeTarget(caster, attackTarget)) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                    else if ((sk.getTargetType() == ESkillTargetType.TARGET_AREA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AREA) && PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && distance <= range) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }
                }
                break;
            }

            default: {
                if (!canAura(sk)) {
                    if (PathFinding.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && distance <= range) {
                        clientStopMoving(null);
                        caster.doCast(sk);
                        return true;
                    }

                    L2Character target = targetReconsider(sk.getCastRange(), true);
                    if (target != null) {
                        clientStopMoving(null);
                        L2Object targets = attackTarget;
                        caster.setTarget(target);
                        caster.doCast(sk);
                        caster.setTarget(targets);
                        return true;
                    }
                }
                else {
                    clientStopMoving(null);
                    caster.doCast(sk);
                    return true;
                }
            }
            break;
        }

        return false;
    }

    /**
     * @param skill the skill to check.
     * @return {@code true} if the skill is available for casting {@code false} otherwise.
     */
    protected boolean checkSkillCastConditions(L2Skill skill) {
        // Not enough MP.
        if (skill.getMpConsume() >= getActiveChar().getCurrentMp()) { return false; }

        // Character is in "skill disabled" mode.
        if (getActiveChar().isSkillDisabled(skill)) { return false; }

        // Is a magic skill and character is magically muted or is a physical skill and character is physically muted.
        if ((skill.isMagic() && getActiveChar().isMuted()) || getActiveChar().isPhysicalMuted()) { return false; }

        return true;
    }

    /**
     * This method checks if the actor will cast a skill or not.
     *
     * @return true if the actor will cast a spell, false otherwise.
     */
    protected boolean willCastASpell() {
        switch (getActiveChar().getAiType()) {
            case HEALER:
            case MAGE:
                return !getActiveChar().isMuted();

            default:
                if (getActiveChar().isPhysicalMuted()) { return false; }
        }
        return Rnd.get(100) < 10;
    }

    /**
     * Method used when the actor can't attack his current target (immobilize state, for exemple).
     * <ul>
     * <li>If the actor got an hate list, pickup a new target from it.</li>
     * <li>If the actor didn't find a target on his hate list, check if he is aggro type and pickup a new target using his knownlist.</li>
     * </ul>
     *
     * @param range      The range to check (skill range for skill ; physical range for melee).
     * @param rangeCheck That boolean is used to see if a check based on the distance must be made (skill check).
     * @return The new L2Character victim.
     */
    protected L2Character targetReconsider(int range, boolean rangeCheck) {
        final L2Attackable actor = getActiveChar();

        // Verify first if aggro list is empty, if not search a victim following his aggro position.
        if (!actor.gotNoTarget()) {
            // Store aggro value && most hated, in order to add it to the random target we will choose.
            final L2Character previousMostHated = actor.getMostHated();
            final int aggroMostHated = actor.getHating(previousMostHated);

            for (L2Character obj : actor.getHateList()) {
                if (!autoAttackCondition(obj)) { continue; }

                if (rangeCheck) {
                    // Verify the distance, -15 if the victim is moving, -15 if the npc is moving.
                    double dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY())) - obj.getTemplate().getCollisionRadius();
                    if (actor.isMoving()) { dist -= 15; }

                    if (obj.isMoving()) { dist -= 15; }

                    if (dist > range) { continue; }
                }

                // Stop to hate the most hated.
                actor.stopHating(previousMostHated);

                // Add previous most hated aggro to that new victim.
                actor.addDamageHate(obj, 0, (aggroMostHated > 0) ? aggroMostHated : 2000);
                return obj;
            }
        }

        // If hate list gave nothing, then verify first if the actor is aggressive, and then pickup a victim from his knownlist.
        if (actor.isAggressive()) {
            for (L2Character target : actor.getKnownList().getKnownTypeInRadius(L2Character.class, actor.getAggroRange())) {
                if (!autoAttackCondition(target)) { continue; }

                if (rangeCheck) {
                    // Verify the distance, -15 if the victim is moving, -15 if the npc is moving.
                    double dist = Math.sqrt(actor.getPlanDistanceSq(target.getX(), target.getY())) - target.getTemplate().getCollisionRadius();
                    if (actor.isMoving()) { dist -= 15; }

                    if (target.isMoving()) { dist -= 15; }

                    if (dist > range) { continue; }
                }

                // Only 1 aggro, as the hate list is supposed to be cleaned. Simulate an aggro range entrance.
                actor.addDamageHate(target, 0, 1);
                return target;
            }
        }

        // Return null if no new victim has been found.
        return null;
    }

    /**
     * Method used for chaotic mode (RBs / GBs and their minions).<br>
     *
     * @param oldTarget The previous target, reused if no available target is found.
     * @return old target if none could fits or the new target.
     */
    protected L2Character aggroReconsider(L2Character oldTarget) {
        final L2Attackable actor = getActiveChar();

        // Choose a new victim, and make checks to see if it fits.
        for (L2Character victim : actor.getHateList()) {
            if (!autoAttackCondition(victim)) { continue; }

            // Add most hated aggro to the victim aggro.
            actor.addDamageHate(victim, 0, actor.getHating(actor.getMostHated()));
            return victim;
        }
        return oldTarget;
    }

    /**
     * Manage AI thinking actions of a L2Attackable.
     */
    @Override
    protected void onEvtThink() {
        // Check if the thinking action is already in progress.
        if (_thinking || actor.isAllSkillsDisabled()) { return; }

        // Start thinking action.
        _thinking = true;

        try {
            // Manage AI thoughts.
            switch (getIntention()) {
                case ACTIVE:
                    thinkActive();
                    break;
                case ATTACK:
                    thinkAttack();
                    break;
                case CAST:
                    thinkCast();
                    break;
            }
        }
        finally {
            // Stop thinking action.
            _thinking = false;
        }
    }

    /**
     * Launch actions corresponding to the Event Attacked.
     * <ul>
     * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
     * <li>Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance</li>
     * <li>Set the Intention to ATTACK</li>
     * </ul>
     *
     * @param attacker The L2Character that attacks the actor
     */
    @Override
    protected void onEvtAttacked(L2Character attacker) {
        final L2Attackable me = getActiveChar();

        // Calculate the attack timeout
        _attackTimeout = System.currentTimeMillis() + MAX_ATTACK_TIMEOUT;

        // Set the _globalAggro to 0 to permit attack even just after spawn
        if (_globalAggro < 0) { _globalAggro = 0; }

        // Add the attacker to the _aggroList of the actor
        me.addDamageHate(attacker, 0, 1);

        // Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
        me.setRunning();

        // Set the Intention to ATTACK
        if (getIntention() != EIntention.ATTACK) { setIntention(EIntention.ATTACK, attacker); }
        else if (me.getMostHated() != getTarget()) { setIntention(EIntention.ATTACK, attacker); }

        if (me instanceof L2MonsterInstance) {
            L2MonsterInstance master = (L2MonsterInstance) me;

            if (master.hasMinions()) { master.getMinionList().onAssist(me, attacker); }
            else {
                master = master.getLeader();
                if (master != null && master.hasMinions()) { master.getMinionList().onAssist(me, attacker); }
            }
        }

        super.onEvtAttacked(attacker);
    }

    /**
     * Launch actions corresponding to the Event Aggression.
     * <ul>
     * <li>Add the target to the actor _aggroList or update hate if already present</li>
     * <li>Set the actor Intention to ATTACK (if actor is L2GuardInstance check if it isn't too far from its home location)</li>
     * </ul>
     *
     * @param target The L2Character that attacks
     * @param aggro  The value of hate to add to the actor against the target
     */
    @Override
    protected void onEvtAggression(L2Character target, int aggro) {
        final L2Attackable me = getActiveChar();

        // Add the target to the actor _aggroList or update hate if already present
        me.addDamageHate(target, 0, aggro);

        // Set the actor AI Intention to ATTACK
        if (getIntention() != EIntention.ATTACK) {
            // Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
            me.setRunning();

            setIntention(EIntention.ATTACK, target);
        }

        if (me instanceof L2MonsterInstance) {
            L2MonsterInstance master = (L2MonsterInstance) me;

            if (master.hasMinions()) { master.getMinionList().onAssist(me, target); }
            else {
                master = master.getLeader();
                if (master != null && master.hasMinions()) { master.getMinionList().onAssist(me, target); }
            }
        }
    }

    @Override
    protected void onIntentionActive() {
        // Cancel attack timeout
        _attackTimeout = Long.MAX_VALUE;

        super.onIntentionActive();
    }

    public void setGlobalAggro(int value) {
        _globalAggro = value;
    }

    private L2Attackable getActiveChar() {
        return (L2Attackable) actor;
    }
}