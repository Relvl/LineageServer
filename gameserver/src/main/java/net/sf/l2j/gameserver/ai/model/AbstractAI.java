package net.sf.l2j.gameserver.ai.model;

import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.NextAction;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

abstract class AbstractAI {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractAI.class);

    private static final int FOLLOW_INTERVAL = 500;

    protected final L2Character actor;
    protected EIntention intention = EIntention.IDLE;
    protected Object intentionArg0;
    protected Object intentionArg1;
    protected volatile boolean actorMoving;
    protected volatile boolean _clientAutoAttacking;

    protected volatile L2Character followTarget;

    protected L2Skill currentlyCastingSkill;
    protected Future<?> followTask;
    private NextAction nextAction;
    private L2Object target;
    private long moveToPawnTimeout;

    protected AbstractAI(L2Character character) {
        if (character == null) {
            throw new IllegalArgumentException("AI's actor cannot be null!");
        }
        actor = character;
    }

    public L2Character getActor() {return actor;}

    public EIntention getIntention() {
        return intention;
    }

    public final void setIntention(EIntention intention) {
        setIntention(intention, null, null);
    }

    public final void setIntention(EIntention intention, Object arg0) {
        setIntention(intention, arg0, null);
    }

    /** FIXME Ебучие наркоманы... Абстракция? Какая, нахуй, обсракция?! */
    public final void setIntention(EIntention intention, Object arg0, Object arg1) {
        if (intention != EIntention.FOLLOW && intention != EIntention.ATTACK) {
            stopFollow();
        }

        switch (intention) {
            case IDLE:
                onIntentionIdle();
                break;
            case ACTIVE:
                onIntentionActive();
                break;
            case REST:
                onIntentionRest();
                break;
            case ATTACK:
                onIntentionAttack((L2Character) arg0);
                break;
            case CAST:
                onIntentionCast((L2Skill) arg0, (L2Object) arg1);
                break;
            case MOVE_TO:
                onIntentionMoveTo((HeadedLocation) arg0);
                break;
            case FOLLOW:
                onIntentionFollow((L2Character) arg0);
                break;
            case PICK_UP:
                onIntentionPickUp((L2Object) arg0);
                break;
            case INTERACT:
                onIntentionInteract((L2Object) arg0);
                break;
        }

        if (nextAction != null && nextAction.getIntention() == intention) {
            nextAction = null;
        }
    }

    protected synchronized void changeIntention(EIntention intention, Object arg0, Object arg1) {
        this.intention = intention;
        this.intentionArg0 = arg0;
        this.intentionArg1 = arg1;
    }

    public final void notifyEvent(ECtrlEvent evt) {
        notifyEvent(evt, null, null);
    }

    public final void notifyEvent(ECtrlEvent evt, Object arg0) {
        notifyEvent(evt, arg0, null);
    }

    public final void notifyEvent(ECtrlEvent evt, Object arg0, Object arg1) {
        if ((!actor.isVisible() && !actor.isTeleporting()) || !actor.hasAI()) {
            return;
        }

        switch (evt) {
            case EVT_THINK:
                onEvtThink();
                break;
            case EVT_ATTACKED:
                onEvtAttacked((L2Character) arg0);
                break;
            case EVT_AGGRESSION:
                onEvtAggression((L2Character) arg0, ((Number) arg1).intValue());
                break;
            case EVT_STUNNED:
                onEvtStunned((L2Character) arg0);
                break;
            case EVT_PARALYZED:
                onEvtParalyzed((L2Character) arg0);
                break;
            case EVT_SLEEPING:
                onEvtSleeping((L2Character) arg0);
                break;
            case EVT_ROOTED:
                onEvtRooted((L2Character) arg0);
                break;
            case EVT_CONFUSED:
                onEvtConfused((L2Character) arg0);
                break;
            case EVT_MUTED:
                onEvtMuted((L2Character) arg0);
                break;
            case EVT_EVADED:
                onEvtEvaded((L2Character) arg0);
                break;
            case EVT_READY_TO_ACT:
                if (!actor.isCastingNow() && !actor.isCastingSimultaneouslyNow()) {
                    onEvtReadyToAct();
                }
                break;
            case EVT_USER_CMD:
                onEvtUserCmd(arg0, arg1);
                break;
            case EVT_ARRIVED:
                if (!actor.isCastingNow() && !actor.isCastingSimultaneouslyNow()) {
                    onEvtArrived();
                }
                break;
            case EVT_ARRIVED_BLOCKED:
                onEvtArrivedBlocked((HeadedLocation) arg0);
                break;
            case EVT_FORGET_OBJECT:
                onEvtForgetObject((L2Object) arg0);
                break;
            case EVT_CANCEL:
                onEvtCancel();
                break;
            case EVT_DEAD:
                onEvtDead();
                break;
            case EVT_FAKE_DEATH:
                onEvtFakeDeath();
                break;
            case EVT_FINISH_CASTING:
                onEvtFinishCasting();
                break;
        }

        // Do next action.
        if (nextAction != null && nextAction.getEvent() == evt) {
            nextAction.run();
        }
    }

    protected abstract void onIntentionIdle();

    protected abstract void onIntentionActive();

    protected abstract void onIntentionRest();

    protected abstract void onIntentionAttack(L2Character target);

    protected abstract void onIntentionCast(L2Skill skill, L2Object target);

    protected abstract void onIntentionMoveTo(HeadedLocation destination);

    protected abstract void onIntentionFollow(L2Character target);

    protected abstract void onIntentionPickUp(L2Object item);

    protected abstract void onIntentionInteract(L2Object object);

    protected abstract void onEvtThink();

    protected abstract void onEvtAttacked(L2Character attacker);

    protected abstract void onEvtAggression(L2Character target, int aggro);

    protected abstract void onEvtStunned(L2Character attacker);

    protected abstract void onEvtParalyzed(L2Character attacker);

    protected abstract void onEvtSleeping(L2Character attacker);

    protected abstract void onEvtRooted(L2Character attacker);

    protected abstract void onEvtConfused(L2Character attacker);

    protected abstract void onEvtMuted(L2Character attacker);

    protected abstract void onEvtEvaded(L2Character attacker);

    protected abstract void onEvtReadyToAct();

    protected abstract void onEvtUserCmd(Object arg0, Object arg1);

    protected abstract void onEvtArrived();

    protected abstract void onEvtArrivedBlocked(HeadedLocation blockedAtPos);

    protected abstract void onEvtForgetObject(L2Object object);

    protected abstract void onEvtCancel();

    protected abstract void onEvtDead();

    protected abstract void onEvtFakeDeath();

    protected abstract void onEvtFinishCasting();

    protected void clientActionFailed() {
        if (actor.isPlayer()) {
            actor.sendPacket(ActionFailed.STATIC_PACKET);
        }
    }

    protected void moveToPawn(L2Object pawn, int offset) {
        if (actorMoving && target == pawn && actor.isOnGeodataPath() && System.currentTimeMillis() < moveToPawnTimeout) {
            return;
        }

        target = pawn;
        if (target == null) { return; }

        moveToPawnTimeout = System.currentTimeMillis() + 2000;

        moveTo(target.getX(), target.getY(), target.getZ(), (offset < 10) ? 10 : offset);
    }

    protected void moveTo(int x, int y, int z) {
        moveTo(x, y, z, 0);
    }

    /**
     * @param destinationRadius Радиус, при пересечении которого считается, что цель достигнута.
     */
    protected void moveTo(int x, int y, int z, int destinationRadius) {
        if (actor.isMovementDisabled()) {
            actor.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        actorMoving = true;
        actor.moveToLocation(x, y, z, destinationRadius);

        if (!actor.isMoving()) {
            actor.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        actor.broadcastPacket(new MoveToLocation(actor));
    }

    protected void clientStopMoving(HeadedLocation pos) {
        if (actor.isMoving()) { actor.stopMove(pos); }

        if (actorMoving || pos != null) {
            actorMoving = false;
            actor.broadcastPacket(new StopMove(actor));

            if (pos != null) {
                actor.broadcastPacket(new StopRotation(actor.getObjectId(), pos.getHeading(), 0));
            }
        }
    }

    public boolean isAutoAttacking() {
        return _clientAutoAttacking;
    }

    public void setAutoAttacking(boolean isAutoAttacking) {
        // Вот это на самом деле не очень понятно, зачем сделано. Владелец не должен начинать автоатаку вместе с автоатакой саммона.
        if (actor.isSummon()) {
            L2Summon summon = (L2Summon) actor;
            if (summon.getOwner() != null) {
                summon.getOwner().getAI().setAutoAttacking(isAutoAttacking);
            }
            return;
        }
        _clientAutoAttacking = isAutoAttacking;
    }

    public void clientStartAutoAttack() {
        if (actor.isSummon()) {
            actor.getActingPlayer().getAI().clientStartAutoAttack();
            return;
        }

        if (!_clientAutoAttacking) {
            if (actor.isPlayer() && actor.getPet() != null) {
                actor.getPet().broadcastPacket(new AutoAttackStart(actor.getPet().getObjectId()));
            }

            actor.broadcastPacket(new AutoAttackStart(actor.getObjectId()));
            setAutoAttacking(true);
        }
        AttackStanceTaskManager.getInstance().add(actor);
    }

    public void clientStopAutoAttack() {
        if (actor.isSummon()) {
            actor.getActingPlayer().getAI().clientStopAutoAttack();
            return;
        }

        if (actor.isPlayer()) {
            if (!AttackStanceTaskManager.getInstance().isInAttackStance(actor) && _clientAutoAttacking) {
                AttackStanceTaskManager.getInstance().add(actor);
            }
        }
        else if (_clientAutoAttacking) {
            actor.broadcastPacket(new AutoAttackStop(actor.getObjectId()));
            setAutoAttacking(false);
        }
    }

    protected void clientNotifyDead() {
        actor.broadcastPacket(new Die(actor));

        intention = EIntention.IDLE;
        target = null;

        stopFollow();
        // Мне кажется, надо еще некоторые моменты прояснить, вроде каста.
    }

    public void describeStateToPlayer(L2PcInstance player) {
        if (actorMoving) {
            player.sendPacket(new MoveToLocation(actor));
        }
    }

    public synchronized void startFollow(L2Character target) {
        startFollow(target, null);
    }

    public synchronized void startFollow(L2Character target, Integer range) {
        if (followTask != null) {
            followTask.cancel(false);
            followTask = null;
        }

        followTarget = target;
        followTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(range), 5, FOLLOW_INTERVAL);
    }

    public synchronized void stopFollow() {
        if (followTask != null) {
            followTask.cancel(false);
            followTask = null;
        }
        followTarget = null;
    }

    protected L2Character getFollowTarget() {return followTarget;}

    public L2Object getTarget() {return target;}

    protected void setTarget(L2Object target) {
        // TODO Подумать о том, что отправку пакетов можно делать отсюда, а не из конечных мест.
        this.target = target;
    }

    public void stopAITask() {stopFollow();}

    public void setNextAction(NextAction nextAction) {this.nextAction = nextAction;}

    @Override
    public String toString() {
        return "AbstractAI{" +
                "actor=" + actor +
                ", intention=" + intention +
                ", intentionArg0=" + intentionArg0 +
                ", intentionArg1=" + intentionArg1 +
                ", actorMoving=" + actorMoving +
                ", _clientAutoAttacking=" + _clientAutoAttacking +
                ", followTarget=" + followTarget +
                ", currentlyCastingSkill=" + currentlyCastingSkill +
                ", followTask=" + followTask +
                ", nextAction=" + nextAction +
                ", target=" + target +
                ", moveToPawnTimeout=" + moveToPawnTimeout +
                '}';
    }

    private class FollowTask implements Runnable {
        protected int range = 70;

        FollowTask(Integer range) {
            this.range = range == null ? 70 : range;
        }

        @Override
        public void run() {
            // target does not exist or is out of max follow/attack/cast range
            if (followTarget == null || !actor.isInsideRadius(followTarget, 3000, true, false)) {
                setIntention(EIntention.IDLE);
                clientActionFailed();
                return;
            }

            // target is not in range, trigger proper AI
            if (!actor.isInsideRadius(followTarget, range, true, false)) {
                if (getIntention() == EIntention.ATTACK || getIntention() == EIntention.CAST) { onEvtThink(); }
                else { moveToPawn(followTarget, range); }
            }
        }
    }
}