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

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;

public class L2SummonAI extends L2PlayableAI implements Runnable
{
	private static final int AVOID_RADIUS = 70;
	
	private volatile boolean _thinking; // to prevent recursive thinking
	private volatile boolean _startFollow = ((L2Summon) actor).getFollowStatus();
	private L2Character _lastAttack = null;
	
	private volatile boolean _startAvoid = false;
	private Future<?> _avoidTask = null;
	
	public L2SummonAI(L2Summon summon)
	{
		super(summon);
	}
	
	@Override
	protected void onIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		onIntentionActive();
	}
	
	@Override
	protected void onIntentionActive()
	{
		L2Summon summon = (L2Summon) actor;
		if (_startFollow)
			setIntention(EIntention.FOLLOW, summon.getOwner());
		else
			super.onIntentionActive();
	}
	
	@Override
protected 	synchronized void changeIntention(EIntention intention, Object arg0, Object arg1)
	{
		switch (intention)
		{
			case ACTIVE:
			case FOLLOW:
				startAvoidTask();
				break;
			default:
				stopAvoidTask();
		}
		
		super.changeIntention(intention, arg0, arg1);
	}
	
	private void thinkAttack()
	{
		L2Character target = (L2Character) getTarget();
		if (target == null)
			return;
		
		if (checkTargetLostOrDead(target))
		{
			setTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, actor.getPhysicalAttackRange()))
		{
			actor.breakAttack();
			return;
		}
		
		clientStopMoving(null);
		actor.doAttack(target);
	}
	
	private void thinkCast()
	{
		if (checkTargetLost(getTarget()))
		{
			setTarget(null);
			return;
		}
		
		boolean val = _startFollow;
		if (maybeMoveToPawn(getTarget(), currentlyCastingSkill.getCastRange()))
			return;
		
		clientStopMoving(null);
		((L2Summon) actor).setFollowStatus(false);
		setIntention(EIntention.IDLE);
		
		_startFollow = val;
		actor.doCast(currentlyCastingSkill);
	}
	
	private void thinkPickUp()
	{
		final L2Object target = getTarget();
		if (checkTargetLost(target))
			return;
		
		if (maybeMoveToPawn(target, 36))
			return;
		
		setIntention(EIntention.IDLE);
		((L2Summon) actor).doPickupItem(target);
	}
	
	private void thinkInteract()
	{
		if (checkTargetLost(getTarget()))
			return;
		
		if (maybeMoveToPawn(getTarget(), 36))
			return;
		
		setIntention(EIntention.IDLE);
	}
	
	@Override
	protected void onEvtThink()
	{
		if (_thinking || actor.isCastingNow() || actor.isAllSkillsDisabled())
			return;
		
		_thinking = true;
		try
		{
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
			_thinking = false;
		}
	}
	
	@Override
	protected void onEvtFinishCasting()
	{
		if (_lastAttack == null)
			((L2Summon) actor).setFollowStatus(_startFollow);
		else
		{
			setIntention(EIntention.ATTACK, _lastAttack);
			_lastAttack = null;
		}
	}
	
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		super.onEvtAttacked(attacker);
		
		avoidAttack(attacker);
	}
	
	@Override
	protected void onEvtEvaded(L2Character attacker)
	{
		super.onEvtEvaded(attacker);
		
		avoidAttack(attacker);
	}
	
	private void avoidAttack(L2Character attacker)
	{
		// trying to avoid if summon near owner
		if (((L2Summon) actor).getOwner() != null && ((L2Summon) actor).getOwner() != attacker && ((L2Summon) actor).getOwner().isInsideRadius(actor, 2 * AVOID_RADIUS, true, false))
			_startAvoid = true;
	}
	
	@Override
	public void run()
	{
		if (_startAvoid)
		{
			_startAvoid = false;
			
			if (!actorMoving && !actor.isDead() && !actor.isMovementDisabled())
			{
				final int ownerX = ((L2Summon) actor).getOwner().getX();
				final int ownerY = ((L2Summon) actor).getOwner().getY();
				final double angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - actor.getY(), ownerX - actor.getX());
				
				final int targetX = ownerX + (int) (AVOID_RADIUS * Math.cos(angle));
				final int targetY = ownerY + (int) (AVOID_RADIUS * Math.sin(angle));
				if (PathFinding.getInstance().canMoveToTarget(actor.getX(), actor.getY(), actor.getZ(), targetX, targetY, actor.getZ()))
					moveTo(targetX, targetY, actor.getZ());
			}
		}
	}
	
	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case ACTIVE:
			case FOLLOW:
			case IDLE:
			case MOVE_TO:
			case PICK_UP:
				((L2Summon) actor).setFollowStatus(_startFollow);
		}
	}
	
	public void setStartFollowController(boolean val)
	{
		_startFollow = val;
	}
	
	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
		if (getIntention() == EIntention.ATTACK)
			_lastAttack = (L2Character) getTarget();
		else
			_lastAttack = null;
		
		super.onIntentionCast(skill, target);
	}
	
	private void startAvoidTask()
	{
		if (_avoidTask == null)
			_avoidTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 100, 100);
	}
	
	private void stopAvoidTask()
	{
		if (_avoidTask != null)
		{
			_avoidTask.cancel(false);
			_avoidTask = null;
		}
	}
	
	@Override
	public void stopAITask()
	{
		stopAvoidTask();
		super.stopAITask();
	}
}