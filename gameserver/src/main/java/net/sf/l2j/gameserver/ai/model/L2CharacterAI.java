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

import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.IntentionCommand;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.client.game_to_client.AutoAttackStop;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class L2CharacterAI extends AbstractAI {
    public L2CharacterAI(L2Character character) {
        super(character);
    }

    public IntentionCommand getNextIntention() {
        return null;
    }

    @Override
    protected void onEvtAttacked(L2Character attacker) {
        if (attacker instanceof L2Attackable && !attacker.isCoreAIDisabled()) { clientStartAutoAttack(); }
    }

    /**
     * Manage the Idle Intention : Stop Attack, Movement and Stand Up the actor.
     * <ul>
     * <li>Set the AI Intention to IDLE</li>
     * <li>Init cast and attack target</li>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Stand up the actor server side AND client side by sending Server->Client packet ChangeWaitType (broadcast)</li>
     * </ul>
     */
    @Override
    protected void onIntentionIdle() {
        // Set the AI Intention to IDLE
        changeIntention(EIntention.IDLE, null, null);

        // Init cast and attack target
        setTarget(null);

        // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
        clientStopMoving(null);

        // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
        clientStopAutoAttack();
    }

    /**
     * Manage the Active Intention : Stop Attack, Movement and Launch Think Event.
     * <ul>
     * <li>Set the AI Intention to ACTIVE</li>
     * <li>Init cast and attack target</li>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Launch the Think Event</li>
     * </ul>
     */
    @Override
    protected void onIntentionActive() {
        // Check if the Intention is not already Active
        if (getIntention() != EIntention.ACTIVE) {
            // Set the AI Intention to ACTIVE
            changeIntention(EIntention.ACTIVE, null, null);

            // Init cast and attack target
            setTarget(null);

            // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
            clientStopMoving(null);

            // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
            clientStopAutoAttack();

            // Also enable random animations for this L2Character if allowed
            // This is only for mobs - town npcs are handled in their constructor
            if (actor instanceof L2Attackable) { ((L2Npc) actor).startRandomAnimationTimer(); }

            // Launch the Think Event
            onEvtThink();
        }
    }

    /**
     * Manage the Rest Intention. Set the AI Intention to IDLE.
     */
    @Override
    protected void onIntentionRest() {
        setIntention(EIntention.IDLE);
    }

    /**
     * Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event.
     * <ul>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Set the Intention of this AI to ATTACK</li>
     * <li>Set or change the AI attack target</li>
     * <li>Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart (broadcast)</li>
     * <li>Launch the Think Event</li>
     * </ul>
     */
    @Override
    protected void onIntentionAttack(L2Character target) {
        if (target == null) {
            clientActionFailed();
            return;
        }

        if (getIntention() == EIntention.REST) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        if (actor.isAllSkillsDisabled() || actor.isCastingNow() || actor.isAfraid()) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        // Check if the Intention is already ATTACK
        if (getIntention() == EIntention.ATTACK) {
            // Check if the AI already targets the L2Character
            if (getTarget() != target) {
                // Set the AI attack target (change target)
                setTarget(target);

                stopFollow();

                // Launch the Think Event
                notifyEvent(ECtrlEvent.EVT_THINK, null);
            }
            else {
                clientActionFailed(); // else client freezes until cancel target
            }
        }
        else {
            // Set the Intention of this AbstractAI to ATTACK
            changeIntention(EIntention.ATTACK, target, null);

            // Set the AI attack target
            setTarget(target);

            stopFollow();

            // Launch the Think Event
            notifyEvent(ECtrlEvent.EVT_THINK, null);
        }
    }

    /**
     * Manage the Cast Intention : Stop current Attack, Init the AI in order to cast and Launch Think Event.
     * <ul>
     * <li>Set the AI cast target</li>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor</li>
     * <li>Set the AI skill used by INTENTION_CAST</li>
     * <li>Set the Intention of this AI to CAST</li>
     * <li>Launch the Think Event</li>
     * </ul>
     */
    @Override
    protected void onIntentionCast(L2Skill skill, L2Object target) {
        if (getIntention() == EIntention.REST && skill.isMagic()) {
            clientActionFailed();
            actor.setIsCastingNow(false);
            return;
        }

        // Set the AI cast target
        setTarget(target);

        // Stop actions client-side to cast the skill
        if (skill.getHitTime() > 50) { actor.abortAttack(); }

        // Set the AI skill used by INTENTION_CAST
        currentlyCastingSkill = skill;

        // Change the Intention of this AbstractAI to CAST
        changeIntention(EIntention.CAST, skill, target);

        // Launch the Think Event
        notifyEvent(ECtrlEvent.EVT_THINK, null);
    }

    /**
     * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.
     * <ul>
     * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Set the Intention of this AI to MOVE_TO</li>
     * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)</li>
     * </ul>
     *
     * @param pos
     */
    @Override
    protected void onIntentionMoveTo(HeadedLocation pos) {
        if (getIntention() == EIntention.REST) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        if (actor.isAllSkillsDisabled() || actor.isCastingNow()) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        // Set the Intention of this AbstractAI to MOVE_TO
        changeIntention(EIntention.MOVE_TO, pos, null);

        // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
        clientStopAutoAttack();

        // Abort the attack of the L2Character and send Server->Client ActionFailed packet
        actor.abortAttack();

        // Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
        moveTo(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Manage the Follow Intention : Stop current Attack and Launch a Follow Task.
     * <ul>
     * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Set the Intention of this AI to FOLLOW</li>
     * <li>Create and Launch an AI Follow Task to execute every 1s</li>
     * </ul>
     */
    @Override
    protected void onIntentionFollow(L2Character target) {
        if (getIntention() == EIntention.REST) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        if (actor.isAllSkillsDisabled() || actor.isCastingNow()) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        if (actor.isMovementDisabled()) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        // Dead actors can`t follow
        if (actor.isDead()) {
            clientActionFailed();
            return;
        }

        // do not follow yourself
        if (actor == target) {
            clientActionFailed();
            return;
        }

        // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
        clientStopAutoAttack();

        // Set the Intention of this AbstractAI to FOLLOW
        changeIntention(EIntention.FOLLOW, target, null);

        // Create and Launch an AI Follow Task to execute every 1s
        startFollow(target);
    }

    /**
     * Manage the PickUp Intention : Set the pick up target and Launch a Move To Pawn Task (offset=20).
     * <ul>
     * <li>Set the AI pick up target</li>
     * <li>Set the Intention of this AI to PICK_UP</li>
     * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
     * </ul>
     */
    @Override
    protected void onIntentionPickUp(L2Object object) {
        // Actor is resting, return.
        if (getIntention() == EIntention.REST) {
            clientActionFailed();
            return;
        }

        // Actor is currently busy casting, return.
        if (actor.isAllSkillsDisabled() || actor.isCastingNow()) {
            clientActionFailed();
            return;
        }

        // Object got strange coords, return.
        if (object.getX() == 0 && object.getY() == 0) {
            clientActionFailed();
            return;
        }

        // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
        clientStopAutoAttack();

        if (object instanceof L2ItemInstance && ((L2ItemInstance) object).getLocation() != EItemLocation.VOID) { return; }

        // Set the Intention of this AbstractAI to PICK_UP
        changeIntention(EIntention.PICK_UP, object, null);

        // Set the AI pick up target
        setTarget(object);

        // Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
        moveToPawn(object, 20);
    }

    /**
     * Manage the Interact Intention : Set the interact target and Launch a Move To Pawn Task (offset=60).
     * <ul>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Set the AI interact target</li>
     * <li>Set the Intention of this AI to INTERACT</li>
     * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
     * </ul>
     */
    @Override
    protected void onIntentionInteract(L2Object object) {
        if (getIntention() == EIntention.REST) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        if (actor.isAllSkillsDisabled() || actor.isCastingNow()) {
            // Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
            clientActionFailed();
            return;
        }

        // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
        clientStopAutoAttack();

        if (getIntention() != EIntention.INTERACT) {
            // Set the Intention of this AbstractAI to INTERACT
            changeIntention(EIntention.INTERACT, object, null);

            // Set the AI interact target
            setTarget(object);

            // Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
            moveToPawn(object, 60);
        }
    }

    @Override
    protected void onEvtThink() {
        // do nothing
    }

    @Override
    protected void onEvtAggression(L2Character target, int aggro) {
        // do nothing
    }

    /**
     * Launch actions corresponding to the Event Stunned then onAttacked Event.
     * <ul>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character</li>
     * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character</li>
     * <li>Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode)</li>
     * </ul>
     */
    @Override
    protected void onEvtStunned(L2Character attacker) {
        // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
        actor.broadcastPacket(new AutoAttackStop(actor.getObjectId()));
        AttackStanceTaskManager.getInstance().remove(actor);

        // Stop Server AutoAttack also
        setAutoAttacking(false);

        // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
        clientStopMoving(null);

        // Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode)
        onEvtAttacked(attacker);
    }

    /**
     * Launch actions corresponding to the Event Paralyzed then onAttacked Event.
     * <ul>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character</li>
     * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character</li>
     * <li>Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode)</li>
     * </ul>
     */
    @Override
    protected void onEvtParalyzed(L2Character attacker) {
        // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
        actor.broadcastPacket(new AutoAttackStop(actor.getObjectId()));
        AttackStanceTaskManager.getInstance().remove(actor);

        // Stop Server AutoAttack also
        setAutoAttacking(false);

        // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
        clientStopMoving(null);

        // Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode)
        onEvtAttacked(attacker);
    }

    /**
     * Launch actions corresponding to the Event Sleeping.
     * <ul>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character</li>
     * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character</li>
     * </ul>
     */
    @Override
    protected void onEvtSleeping(L2Character attacker) {
        // Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
        actor.broadcastPacket(new AutoAttackStop(actor.getObjectId()));
        AttackStanceTaskManager.getInstance().remove(actor);

        // stop Server AutoAttack also
        setAutoAttacking(false);

        // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
        clientStopMoving(null);
    }

    /**
     * Launch actions corresponding to the Event Rooted.
     * <ul>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Launch actions corresponding to the Event onAttacked</li>
     * </ul>
     */
    @Override
    protected void onEvtRooted(L2Character attacker) {
        // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
        clientStopMoving(null);

        // Launch actions corresponding to the Event onAttacked
        onEvtAttacked(attacker);
    }

    /**
     * Launch actions corresponding to the Event Confused.
     * <ul>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Launch actions corresponding to the Event onAttacked</li>
     * </ul>
     */
    @Override
    protected void onEvtConfused(L2Character attacker) {
        // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
        clientStopMoving(null);

        // Launch actions corresponding to the Event onAttacked
        onEvtAttacked(attacker);
    }

    /**
     * Launch actions corresponding to the Event Muted.
     * <ul>
     * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character</li>
     * </ul>
     */
    @Override
    protected void onEvtMuted(L2Character attacker) {
        // Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character
        onEvtAttacked(attacker);
    }

    @Override
    protected void onEvtEvaded(L2Character attacker) {
        // do nothing
    }

    /**
     * Launch actions corresponding to the Event ReadyToAct.
     * <ul>
     * <li>Launch actions corresponding to the Event Think</li>
     * </ul>
     */
    @Override
    protected void onEvtReadyToAct() {
        // Launch actions corresponding to the Event Think
        onEvtThink();
    }

    @Override
    protected void onEvtUserCmd(Object arg0, Object arg1) {
        // do nothing
    }

    /**
     * Launch actions corresponding to the Event Arrived.
     * <ul>
     * <li>If the Intention was MOVE_TO, set the Intention to ACTIVE</li>
     * <li>Launch actions corresponding to the Event Think</li>
     * </ul>
     */
    @Override
    protected void onEvtArrived() {
        actor.revalidateZone(true);

        if (actor.moveToNextRoutePoint()) { return; }

        if (actor instanceof L2Attackable) { ((L2Attackable) actor).setIsReturningToSpawnPoint(false); }

        actorMoving = false;

        // If the Intention was MOVE_TO, set the Intention to ACTIVE
        if (getIntention() == EIntention.MOVE_TO) { setIntention(EIntention.ACTIVE); }

        // Launch actions corresponding to the Event Think
        onEvtThink();
    }

    /**
     * Launch actions corresponding to the Event ArrivedBlocked.
     * <ul>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>If the Intention was MOVE_TO, set the Intention to ACTIVE</li>
     * <li>Launch actions corresponding to the Event Think</li>
     * </ul>
     *
     * @param blockedAtPos
     */
    @Override
    protected void onEvtArrivedBlocked(HeadedLocation blockedAtPos) {
        // If the Intention was MOVE_TO, set the Intention to ACTIVE
        if (getIntention() == EIntention.MOVE_TO || getIntention() == EIntention.CAST) { setIntention(EIntention.ACTIVE); }

        // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
        clientStopMoving(blockedAtPos);

        // Launch actions corresponding to the Event Think
        onEvtThink();
    }

    /**
     * Launch actions corresponding to the Event ForgetObject.
     * <ul>
     * <li>If the object was targeted and the Intention was INTERACT or PICK_UP, set the Intention to ACTIVE</li>
     * <li>If the object was targeted to attack, stop the auto-attack, cancel target and set the Intention to ACTIVE</li>
     * <li>If the object was targeted to cast, cancel target and set the Intention to ACTIVE</li>
     * <li>If the object was targeted to follow, stop the movement, cancel AI Follow Task and set the Intention to ACTIVE</li>
     * <li>If the targeted object was the actor , cancel AI target, stop AI Follow Task, stop the movement and set the Intention to IDLE</li>
     * </ul>
     */
    @Override
    protected void onEvtForgetObject(L2Object object) {
        // Check if the object was targeted to attack
        if (getTarget() == object) {
            // Cancel attack target
            setTarget(null);

            // Set the Intention of this AbstractAI to ACTIVE
            setIntention(EIntention.ACTIVE);
        }

        // Check if the object was targeted to follow
        if (getFollowTarget() == object) {
            // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
            clientStopMoving(null);

            // Stop an AI Follow Task
            stopFollow();

            // Set the Intention of this AbstractAI to ACTIVE
            setIntention(EIntention.ACTIVE);
        }

        // Check if the targeted object was the actor
        if (actor == object) {
            // Cancel AI target
            setTarget(null);

            // Stop an AI Follow Task
            stopFollow();

            // Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
            clientStopMoving(null);

            // Set the Intention of this AbstractAI to IDLE
            changeIntention(EIntention.IDLE, null, null);
        }
    }

    /**
     * Launch actions corresponding to the Event Cancel.
     * <ul>
     * <li>Stop an AI Follow Task</li>
     * <li>Launch actions corresponding to the Event Think</li>
     * </ul>
     */
    @Override
    protected void onEvtCancel() {
        actor.abortCast();

        // Stop an AI Follow Task
        stopFollow();

        if (!AttackStanceTaskManager.getInstance().isInAttackStance(actor)) { actor.broadcastPacket(new AutoAttackStop(actor.getObjectId())); }

        // Launch actions corresponding to the Event Think
        onEvtThink();
    }

    /**
     * Launch actions corresponding to the Event Dead.
     * <ul>
     * <li>Stop an AI Follow Task</li>
     * <li>Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)</li>
     * </ul>
     */
    @Override
    protected void onEvtDead() {
        // Stop an AI Tasks
        stopAITask();

        // Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)
        clientNotifyDead();

        if (!(actor instanceof L2Playable)) { actor.setWalking(); }
    }

    /**
     * Launch actions corresponding to the Event Fake Death.
     * <ul>
     * <li>Stop an AI Follow Task</li>
     * </ul>
     */
    @Override
    protected void onEvtFakeDeath() {
        // Stop an AI Follow Task
        stopFollow();

        // Stop the actor movement and send Server->Client packet StopMove/StopRotation (broadcast)
        clientStopMoving(null);

        // Init AI
        intention = EIntention.IDLE;
        setTarget(null);
    }

    @Override
    protected void onEvtFinishCasting() {
        // do nothing
    }

    protected boolean maybeMoveToPosition(Location worldPosition, int offset) {
        if (worldPosition == null) {
            LOGGER.warn("maybeMoveToPosition: worldPosition == NULL!");
            return false;
        }

        if (offset < 0) {
            return false; // skill radius -1
        }

        if (!actor.isInsideRadius(worldPosition.getX(), worldPosition.getY(), offset + actor.getTemplate().getCollisionRadius(), false)) {
            if (actor.isMovementDisabled()) { return true; }

            if (!(this instanceof L2PlayerAI) && !(this instanceof L2SummonAI)) { actor.setRunning(); }

            stopFollow();

            int x = actor.getX();
            int y = actor.getY();

            double dx = worldPosition.getX() - x;
            double dy = worldPosition.getY() - y;

            double dist = Math.sqrt(dx * dx + dy * dy);

            double sin = dy / dist;
            double cos = dx / dist;

            dist -= offset - 5;

            x += (int) (dist * cos);
            y += (int) (dist * sin);

            moveTo(x, y, worldPosition.getZ());
            return true;
        }

        if (getFollowTarget() != null) { stopFollow(); }

        return false;
    }

    /**
     * Manage the Move to Pawn action in function of the distance and of the Interact area.
     * <ul>
     * <li>Get the distance between the current position of the L2Character and the target (x,y)</li>
     * <li>If the distance > offset+20, move the actor (by running) to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
     * <li>If the distance <= offset+20, Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * </ul>
     *
     * @param target The targeted L2Object
     * @param offset The Interact area radius
     * @return True if a movement must be done
     */
    protected boolean maybeMoveToPawn(L2Object target, int offset) {
        if (target == null || offset < 0) // skill radius -1
        { return false; }

        offset += actor.getTemplate().getCollisionRadius();
        if (target instanceof L2Character) { offset += ((L2Character) target).getTemplate().getCollisionRadius(); }

        if (!actor.isInsideRadius(target, offset, false, false)) {
            if (getFollowTarget() != null) {
                int foffset = offset + (((L2Character) target).isMoving() ? 100 : 0);

                // allow larger hit range when the target is moving (check is run only once per second)
                if (!actor.isInsideRadius(target, foffset, false, false)) {
                    if (!actor.isAttackingNow() || actor instanceof L2Summon) { moveToPawn(target, offset); }

                    return true;
                }

                stopFollow();
                return false;
            }

            if (actor.isMovementDisabled() && !(actor instanceof L2Attackable)) {
                if (getIntention() == EIntention.ATTACK) { setIntention(EIntention.IDLE); }

                return true;
            }

            // If not running, set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
            if (!(this instanceof L2PlayerAI) && !(this instanceof L2SummonAI)) { actor.setRunning(); }

            if ((target instanceof L2Character) && !(target instanceof L2DoorInstance)) { startFollow((L2Character) target, offset); }
            else {
                // Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
                moveToPawn(target, offset);
            }
            return true;
        }

        stopFollow();
        return false;
    }

    /**
     * Modify current Intention and actions if the target is lost or dead.
     * <ul>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Set the Intention of this AbstractAI to ACTIVE</li>
     * </ul>
     *
     * @param target The targeted L2Object
     * @return True if the target is lost or dead (false if fakedeath)
     */
    protected boolean checkTargetLostOrDead(L2Character target) {
        if (target == null || target.isAlikeDead()) {
            if (target instanceof L2PcInstance && ((L2PcInstance) target).isFakeDeath()) {
                target.stopFakeDeath(true);
                return false;
            }

            // Set the Intention of this AbstractAI to ACTIVE
            setIntention(EIntention.ACTIVE);
            return true;
        }
        return false;
    }

    /**
     * Modify current Intention and actions if the target is lost.
     * <ul>
     * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
     * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
     * <li>Set the Intention of this AbstractAI to ACTIVE</li>
     * </ul>
     *
     * @param target The targeted L2Object
     * @return True if the target is lost
     */
    protected boolean checkTargetLost(L2Object target) {
        if (target instanceof L2PcInstance) {
            L2PcInstance victim = (L2PcInstance) target;
            if (victim.isFakeDeath()) {
                victim.stopFakeDeath(true);
                return false;
            }
        }

        if (target == null) {
            // Set the Intention of this AbstractAI to ACTIVE
            setIntention(EIntention.ACTIVE);
            return true;
        }
        return false;
    }

    public boolean canAura(L2Skill sk) {
        if (sk.getTargetType() == ESkillTargetType.TARGET_AURA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AURA) {
            for (L2Object target : actor.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius())) {
                if (target == getTarget()) { return true; }
            }
        }
        return false;
    }

    public boolean canAOE(L2Skill sk) {
        if (sk.getSkillType() != L2SkillType.NEGATE || sk.getSkillType() != L2SkillType.CANCEL) {
            if (sk.getTargetType() == ESkillTargetType.TARGET_AURA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AURA) {
                boolean cancast = true;
                for (L2Character target : actor.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius())) {
                    if (!PathFinding.getInstance().canSeeTarget(actor, target)) { continue; }

                    if (target instanceof L2Attackable && !actor.isConfused()) { continue; }

                    if (target.getFirstEffect(sk) != null) { cancast = false; }
                }

                if (cancast) { return true; }
            }
            else if (sk.getTargetType() == ESkillTargetType.TARGET_AREA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AREA) {
                boolean cancast = true;
                for (L2Character target : ((L2Character) getTarget()).getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius())) {
                    if (!PathFinding.getInstance().canSeeTarget(actor, target)) { continue; }

                    if (target instanceof L2Attackable && !actor.isConfused()) { continue; }

                    L2Effect[] effects = target.getAllEffects();
                    if (effects.length > 0) { cancast = true; }
                }
                if (cancast) { return true; }
            }
        }
        else {
            if (sk.getTargetType() == ESkillTargetType.TARGET_AURA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AURA) {
                boolean cancast = false;
                for (L2Character target : actor.getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius())) {
                    if (!PathFinding.getInstance().canSeeTarget(actor, target)) { continue; }

                    if (target instanceof L2Attackable && !actor.isConfused()) { continue; }

                    L2Effect[] effects = target.getAllEffects();
                    if (effects.length > 0) { cancast = true; }
                }
                if (cancast) { return true; }
            }
            else if (sk.getTargetType() == ESkillTargetType.TARGET_AREA || sk.getTargetType() == ESkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == ESkillTargetType.TARGET_FRONT_AREA) {
                boolean cancast = true;
                for (L2Character target : ((L2Character) getTarget()).getKnownList().getKnownTypeInRadius(L2Character.class, sk.getSkillRadius())) {
                    if (!PathFinding.getInstance().canSeeTarget(actor, target)) { continue; }

                    if (target instanceof L2Attackable && !actor.isConfused()) { continue; }

                    if (target.getFirstEffect(sk) != null) { cancast = false; }
                }

                if (cancast) { return true; }
            }
        }
        return false;
    }

    public boolean canParty(L2Skill sk) {
        if (sk.getTargetType() != ESkillTargetType.TARGET_PARTY) { return false; }

        int count = 0;
        int ccount = 0;

        String[] actorClans = ((L2Npc) actor).getClans();
        for (L2Attackable target : actor.getKnownList().getKnownTypeInRadius(L2Attackable.class, sk.getSkillRadius())) {
            if (!PathFinding.getInstance().canSeeTarget(actor, target)) { continue; }

            if (!Util.contains(actorClans, target.getClans())) { continue; }

            count++;

            if (target.getFirstEffect(sk) != null) { ccount++; }
        }

        return ccount < count;

    }
}