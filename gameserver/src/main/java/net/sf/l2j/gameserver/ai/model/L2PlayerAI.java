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

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.IntentionCommand;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;

public class L2PlayerAI extends L2PlayableAI
{
	private boolean _thinking; // to prevent recursive thinking
	private IntentionCommand _nextIntention = null;
	
	public L2PlayerAI(L2PcInstance player)
	{
		super(player);
	}
	
	void setNextIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_nextIntention = new IntentionCommand(intention, arg0, arg1);
	}
	
	@Override
	public IntentionCommand getNextIntention()
	{
		return _nextIntention;
	}
	
	/**
	 * Saves the current Intention for this L2PlayerAI if necessary and calls changeIntention in AbstractAI.<BR>
	 * <BR>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// do nothing unless CAST intention
		// however, forget interrupted actions when starting to use an offensive skill
		if (intention != CtrlIntention.CAST || (arg0 != null && ((L2Skill) arg0).isOffensive()))
		{
			_nextIntention = null;
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// do nothing if next intention is same as current one.
		if (intention == _intention && arg0 == _intentionArg0 && arg1 == _intentionArg1)
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// save current intention so it can be used after cast
		setNextIntention(_intention, _intentionArg0, _intentionArg1);
		super.changeIntention(intention, arg0, arg1);
	}
	
	/**
	 * Launch actions corresponding to the Event ReadyToAct.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Launch actions corresponding to the Event Think</li><BR>
	 * <BR>
	 */
	@Override
	protected void onEvtReadyToAct()
	{
		// Launch actions corresponding to the Event Think
		if (_nextIntention != null)
		{
			setIntention(_nextIntention.getCtrlIntention(), _nextIntention.getFirstArgument(), _nextIntention.getSecondArgument());
			_nextIntention = null;
		}
		super.onEvtReadyToAct();
	}
	
	@Override
	protected void onEvtCancel()
	{
		_nextIntention = null;
		super.onEvtCancel();
	}
	
	/**
	 * Finalize the casting of a skill. Drop latest intention before the actual CAST.
	 */
	@Override
	protected void onEvtFinishCasting()
	{
		if (getIntention() == CtrlIntention.CAST)
		{
			if (_nextIntention != null && _nextIntention.getCtrlIntention() != CtrlIntention.CAST) // previous state shouldn't be casting
				setIntention(_nextIntention.getCtrlIntention(), _nextIntention.getFirstArgument(), _nextIntention.getSecondArgument());
			else
				setIntention(CtrlIntention.IDLE);
		}
	}
	
	@Override
	protected void onIntentionRest()
	{
		if (getIntention() != CtrlIntention.REST)
		{
			changeIntention(CtrlIntention.REST, null, null);
			setTarget(null);
			clientStopMoving(null);
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		setIntention(CtrlIntention.IDLE);
	}
	
	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> : </B><BR>
	 * <BR>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li> <li>Set the Intention of this AI to MOVE_TO</li> <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation
	 * (broadcast)</li><BR>
	 * <BR>
	 */
	@Override
	protected void onIntentionMoveTo(L2CharPosition pos)
	{
		if (getIntention() == CtrlIntention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAttackingNow())
		{
			clientActionFailed();
			setNextIntention(CtrlIntention.MOVE_TO, pos, null);
			return;
		}
		
		// Set the Intention of this AbstractAI to MOVE_TO
		changeIntention(CtrlIntention.MOVE_TO, pos, null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Abort the attack of the L2Character and send Server->Client ActionFailed packet
		_actor.abortAttack();
		
		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
		moveTo(pos.x, pos.y, pos.z);
	}
	
	@Override
	protected void clientNotifyDead()
	{
		_clientMoving = false;
		
		super.clientNotifyDead();
	}
	
	private void thinkAttack()
	{
		L2Character target = (L2Character) getTarget();
		if (target == null)
			return;
		
		if (checkTargetLostOrDead(target))
		{
			// Notify the target
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
			return;
		
		clientStopMoving(null);
		_actor.doAttack(target);
	}
	
	private void thinkCast()
	{
		L2Character target = (L2Character) getTarget();
		
		if (_skill.getTargetType() == SkillTargetType.TARGET_GROUND && _actor instanceof L2PcInstance)
		{
			if (maybeMoveToPosition(((L2PcInstance) _actor).getCurrentSkillWorldPosition(), _skill.getCastRange()))
			{
				_actor.setIsCastingNow(false);
				return;
			}
		}
		else
		{
			if (checkTargetLost(target))
			{
				// Notify the target
				if (_skill.isOffensive() && getTarget() != null)
					setTarget(null);
				
				_actor.setIsCastingNow(false);
				return;
			}
			
			if (target != null && maybeMoveToPawn(target, _skill.getCastRange()))
			{
				_actor.setIsCastingNow(false);
				return;
			}
		}
		
		if (!_skill.isToggle())
			clientStopMoving(null);
		
		L2Object oldTarget = _actor.getTarget();
		if (oldTarget != null && target != null && oldTarget != target)
		{
			// Replace the current target by the cast target
			_actor.setTarget(getTarget());
			// Launch the Cast of the skill
			_actor.doCast(_skill);
			// Restore the initial target
			_actor.setTarget(oldTarget);
		}
		else
			_actor.doCast(_skill);
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			return;
		
		final L2Object target = getTarget();
		if (checkTargetLost(target))
			return;
		
		if (maybeMoveToPawn(target, 36))
			return;
		
		setIntention(CtrlIntention.IDLE);
		_actor.getActingPlayer().doPickupItem(target);
	}
	
	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
			return;
		
		L2Object target = getTarget();
		if (checkTargetLost(target))
			return;
		
		if (maybeMoveToPawn(target, 36))
			return;
		
		if (!(target instanceof L2StaticObjectInstance))
			_actor.getActingPlayer().doInteract((L2Character) target);
		
		setIntention(CtrlIntention.IDLE);
	}
	
	@Override
	protected void onEvtThink()
	{
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (_thinking && getIntention() != CtrlIntention.CAST) // casting must always continue
			return;
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thoughts
			switch (getIntention())
			{
				case ATTACK:
					thinkAttack();
					break;
				case CAST:
					thinkCast();
					break;
				case PICK_UP:
					thinkPickUp();
					break;
				case INTERACT:
					thinkInteract();
					break;
			}
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}
}