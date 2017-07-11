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

import java.util.concurrent.Future;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.Ctrl;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.NextAction;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.AutoAttackStart;
import net.sf.l2j.gameserver.network.client.game_to_client.AutoAttackStop;
import net.sf.l2j.gameserver.network.client.game_to_client.Die;
import net.sf.l2j.gameserver.network.client.game_to_client.MoveToLocation;
import net.sf.l2j.gameserver.network.client.game_to_client.StopMove;
import net.sf.l2j.gameserver.network.client.game_to_client.StopRotation;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

abstract class AbstractAI implements Ctrl
{
	protected static final Logger _log = Logger.getLogger(AbstractAI.class.getName());
	
	/** The character that this AI manages */
	protected final L2Character _actor;
	
	/** Current long-term intention */
	protected CtrlIntention _intention = CtrlIntention.IDLE;
	protected Object _intentionArg0 = null;
	protected Object _intentionArg1 = null;
	
	private NextAction _nextAction;
	
	/** Flags about client's state, in order to know which messages to send */
	protected volatile boolean _clientMoving;
	protected volatile boolean _clientAutoAttacking;
	
	/** Different targets this AI maintains */
	private L2Object _target;
	protected L2Character _followTarget;
	
	/** The skill we are currently casting by INTENTION_CAST */
	protected L2Skill _skill;
	
	/** Different internal state flags */
	private long _moveToPawnTimeout;
	
	protected Future<?> _followTask = null;
	private static final int FOLLOW_INTERVAL = 1000;
	private static final int ATTACK_FOLLOW_INTERVAL = 500;
	
	protected AbstractAI(L2Character character)
	{
		_actor = character;
	}
	
	/**
	 * Return the L2Character managed by this Accessor AI.
	 */
	@Override
	public L2Character getActor()
	{
		return _actor;
	}
	
	/**
	 * Return the current Intention.
	 */
	@Override
	public CtrlIntention getIntention()
	{
		return _intention;
	}
	
	/**
	 * Set the Intention of this AbstractAI.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is USED by AI classes</B></FONT><BR>
	 * <BR>
	 * <B><U> Overridden in </U> : </B><BR>
	 * <B>L2AttackableAI</B> : Create an AI Task executed every 1s (if necessary)<BR>
	 * <B>L2PlayerAI</B> : Stores the current AI intention parameters to later restore it if necessary<BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_intention = intention;
		_intentionArg0 = arg0;
		_intentionArg1 = arg1;
	}
	
	/**
	 * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 */
	@Override
	public final void setIntention(CtrlIntention intention)
	{
		setIntention(intention, null, null);
	}
	
	/**
	 * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention (optional target)
	 */
	@Override
	public final void setIntention(CtrlIntention intention, Object arg0)
	{
		setIntention(intention, arg0, null);
	}
	
	/**
	 * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention (optional target)
	 * @param arg1 The second parameter of the Intention (optional target)
	 */
	@Override
	public final void setIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// Stop the follow mode if necessary
		if (intention != CtrlIntention.FOLLOW && intention != CtrlIntention.ATTACK)
			stopFollow();
		
		// Launch the onIntention method of the L2CharacterAI corresponding to the new Intention
		switch (intention)
		{
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
				onIntentionMoveTo((L2CharPosition) arg0);
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
		
		// If do move or follow intention drop next action.
		if (_nextAction != null && _nextAction.getIntention() == intention)
			_nextAction = null;
	}
	
	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR>
	 * <BR>
	 * @param evt The event whose the AI must be notified
	 */
	@Override
	public final void notifyEvent(CtrlEvent evt)
	{
		notifyEvent(evt, null, null);
	}
	
	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR>
	 * <BR>
	 * @param evt The event whose the AI must be notified
	 * @param arg0 The first parameter of the Event (optional target)
	 */
	@Override
	public final void notifyEvent(CtrlEvent evt, Object arg0)
	{
		notifyEvent(evt, arg0, null);
	}
	
	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR>
	 * <BR>
	 * @param evt The event whose the AI must be notified
	 * @param arg0 The first parameter of the Event (optional target)
	 * @param arg1 The second parameter of the Event (optional target)
	 */
	@Override
	public final void notifyEvent(CtrlEvent evt, Object arg0, Object arg1)
	{
		if ((!_actor.isVisible() && !_actor.isTeleporting()) || !_actor.hasAI())
			return;
		
		switch (evt)
		{
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
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
					onEvtReadyToAct();
				break;
			case EVT_USER_CMD:
				onEvtUserCmd(arg0, arg1);
				break;
			case EVT_ARRIVED:
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
					onEvtArrived();
				break;
			case EVT_ARRIVED_BLOCKED:
				onEvtArrivedBlocked((L2CharPosition) arg0);
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
		if (_nextAction != null && _nextAction.getEvent() == evt)
			_nextAction.run();
	}
	
	protected abstract void onIntentionIdle();
	
	protected abstract void onIntentionActive();
	
	protected abstract void onIntentionRest();
	
	protected abstract void onIntentionAttack(L2Character target);
	
	protected abstract void onIntentionCast(L2Skill skill, L2Object target);
	
	protected abstract void onIntentionMoveTo(L2CharPosition destination);
	
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
	
	protected abstract void onEvtArrivedBlocked(L2CharPosition blocked_at_pos);
	
	protected abstract void onEvtForgetObject(L2Object object);
	
	protected abstract void onEvtCancel();
	
	protected abstract void onEvtDead();
	
	protected abstract void onEvtFakeDeath();
	
	protected abstract void onEvtFinishCasting();
	
	/**
	 * Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	protected void clientActionFailed()
	{
		if (_actor instanceof L2PcInstance)
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 * @param pawn
	 * @param offset
	 */
	protected void moveToPawn(L2Object pawn, int offset)
	{
		if (_clientMoving && _target == pawn && _actor.isOnGeodataPath() && System.currentTimeMillis() < _moveToPawnTimeout)
			return;
		
		_target = pawn;
		if (_target == null)
			return;
		
		_moveToPawnTimeout = System.currentTimeMillis() + 2000;
		
		moveTo(_target.getX(), _target.getY(), _target.getZ(), (offset < 10) ? 10 : offset);
	}
	
	/**
	 * Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 * @param x
	 * @param y
	 * @param z
	 */
	protected void moveTo(int x, int y, int z)
	{
		moveTo(x, y, z, 0);
	}
	
	/**
	 * Move the actor to Location (x,y,z,offset) server side AND client side by sending Server->Client packet CharMoveToLocation <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 * @param x
	 * @param y
	 * @param z
	 * @param offset
	 */
	protected void moveTo(int x, int y, int z, int offset)
	{
		// Check if actor can move
		if (_actor.isMovementDisabled())
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Set AI movement data
		_clientMoving = true;
		
		// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
		_actor.moveToLocation(x, y, z, offset);
		
		if (!_actor.isMoving())
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
		_actor.broadcastPacket(new MoveToLocation(_actor));
	}
	
	/**
	 * Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 * @param pos
	 */
	protected void clientStopMoving(L2CharPosition pos)
	{
		// Stop movement of the L2Character
		if (_actor.isMoving())
			_actor.stopMove(pos);
		
		if (_clientMoving || pos != null)
		{
			_clientMoving = false;
			_actor.broadcastPacket(new StopMove(_actor));
			
			if (pos != null)
				_actor.broadcastPacket(new StopRotation(_actor.getObjectId(), pos.heading, 0));
		}
	}
	
	// Client has already arrived to target, no need to force StopMove packet
	protected void clientStoppedMoving()
	{
		_clientMoving = false;
	}
	
	public boolean isAutoAttacking()
	{
		return _clientAutoAttacking;
	}
	
	public void setAutoAttacking(boolean isAutoAttacking)
	{
		if (_actor instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) _actor;
			if (summon.getOwner() != null)
				summon.getOwner().getAI().setAutoAttacking(isAutoAttacking);
			return;
		}
		_clientAutoAttacking = isAutoAttacking;
	}
	
	/**
	 * Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	public void clientStartAutoAttack()
	{
		if (_actor instanceof L2Summon)
		{
			_actor.getActingPlayer().getAI().clientStartAutoAttack();
			return;
		}
		
		if (!isAutoAttacking())
		{
			if (_actor instanceof L2PcInstance && ((L2PcInstance) _actor).getPet() != null)
				((L2PcInstance) _actor).getPet().broadcastPacket(new AutoAttackStart(((L2PcInstance) _actor).getPet().getObjectId()));
			
			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
			setAutoAttacking(true);
		}
		AttackStanceTaskManager.getInstance().add(_actor);
	}
	
	/**
	 * Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 */
	public void clientStopAutoAttack()
	{
		if (_actor instanceof L2Summon)
		{
			_actor.getActingPlayer().getAI().clientStopAutoAttack();
			return;
		}
		
		if (_actor instanceof L2PcInstance)
		{
			if (!AttackStanceTaskManager.getInstance().isInAttackStance(_actor) && isAutoAttacking())
				AttackStanceTaskManager.getInstance().add(_actor);
		}
		else if (isAutoAttacking())
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
			setAutoAttacking(false);
		}
	}
	
	/**
	 * Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	protected void clientNotifyDead()
	{
		// Send a Server->Client packet Die to the actor and all L2PcInstance in its _knownPlayers
		_actor.broadcastPacket(new Die(_actor));
		
		// Init AI
		_intention = CtrlIntention.IDLE;
		_target = null;
		
		// Cancel the follow task if necessary
		stopFollow();
	}
	
	/**
	 * Update the state of this actor client side by sending Server->Client packet MoveToPawn/MoveToLocation and AutoAttackStart to the L2PcInstance player.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 * @param player The L2PcIstance to notify with state of this L2Character
	 */
	public void describeStateToPlayer(L2PcInstance player)
	{
		if (_clientMoving)
			player.sendPacket(new MoveToLocation(_actor));
	}
	
	/**
	 * Create and Launch an AI Follow Task to execute every 1s.<BR>
	 * <BR>
	 * @param target The L2Character to follow
	 */
	public synchronized void startFollow(L2Character target)
	{
		if (_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}
		
		// Create and Launch an AI Follow Task to execute every 1s
		_followTarget = target;
		_followTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(), 5, FOLLOW_INTERVAL);
	}
	
	/**
	 * Create and Launch an AI Follow Task to execute every 0.5s, following at specified range.
	 * @param target The L2Character to follow
	 * @param range
	 */
	public synchronized void startFollow(L2Character target, int range)
	{
		if (_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}
		
		_followTarget = target;
		_followTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(range), 5, ATTACK_FOLLOW_INTERVAL);
	}
	
	/**
	 * Stop an AI Follow Task.
	 */
	public synchronized void stopFollow()
	{
		if (_followTask != null)
		{
			// Stop the Follow Task
			_followTask.cancel(false);
			_followTask = null;
		}
		_followTarget = null;
	}
	
	protected L2Character getFollowTarget()
	{
		return _followTarget;
	}
	
	public L2Object getTarget()
	{
		return _target;
	}
	
	protected void setTarget(L2Object target)
	{
		_target = target;
	}
	
	/**
	 * Stop all Ai tasks and futures.
	 */
	public void stopAITask()
	{
		stopFollow();
	}
	
	/**
	 * @param nextAction the _nextAction to set
	 */
	public void setNextAction(NextAction nextAction)
	{
		_nextAction = nextAction;
	}
	
	@Override
	public String toString()
	{
		return (_actor == null) ? "Actor: null" : "Actor: " + _actor;
	}
	
	class FollowTask implements Runnable
	{
		protected int _range = 70;
		
		public FollowTask()
		{
		}
		
		public FollowTask(int range)
		{
			_range = range;
		}
		
		@Override
		public void run()
		{
			// get target
			L2Character followTarget = _followTarget;
			
			// target does not exist or is out of max follow/attack/cast range
			if (followTarget == null || !_actor.isInsideRadius(followTarget, 3000, true, false))
			{
				setIntention(CtrlIntention.IDLE);
				clientActionFailed();
				return;
			}
			
			// target is not in range, trigger proper AI
			if (!_actor.isInsideRadius(followTarget, _range, true, false))
			{
				if (getIntention() == CtrlIntention.ATTACK || getIntention() == CtrlIntention.CAST)
					onEvtThink();
				else
					moveToPawn(followTarget, _range);
			}
		}
	}
}