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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * A BabyPet can heal his owner. It got 2 heal power, weak or strong.
 * <ul>
 * <li>If the owner's HP is more than 80%, do nothing.</li>
 * <li>If the owner's HP is under 15%, have 75% chances of using a strong heal.</li>
 * <li>Otherwise, have 25% chances for weak heal.</li>
 * </ul>
 */
public final class L2BabyPetInstance extends L2PetInstance
{
	protected IntIntHolder _majorHeal = null;
	protected IntIntHolder _minorHeal = null;
	
	private Future<?> _castTask;
	
	public L2BabyPetInstance(int objectId, NpcTemplate template, L2PcInstance owner, ItemInstance control)
	{
		super(objectId, template, owner, control);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		double healPower = 0;
		int skillLevel;
		for (L2Skill skill : getTemplate().getHealSkills())
		{
			if (skill.getTargetType() != SkillTargetType.TARGET_OWNER_PET || skill.getSkillType() != L2SkillType.HEAL)
				continue;
			
			// The skill level is calculated on the fly. Template got an skill level of 1.
			skillLevel = getSkillLevel(skill.getId());
			if (skillLevel <= 0)
				continue;
			
			if (healPower == 0)
			{
				// set both heal types to the same skill
				_majorHeal = new IntIntHolder(skill.getId(), skillLevel);
				_minorHeal = _majorHeal;
				healPower = skill.getPower();
			}
			else
			{
				// another heal skill found - search for most powerful
				if (skill.getPower() > healPower)
					_majorHeal = new IntIntHolder(skill.getId(), skillLevel);
				else
					_minorHeal = new IntIntHolder(skill.getId(), skillLevel);
			}
		}
		startCastTask();
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		stopCastTask();
		abortCast();
		return true;
	}
	
	@Override
	public synchronized void unSummon(L2PcInstance owner)
	{
		stopCastTask();
		abortCast();
		
		super.unSummon(owner);
	}
	
	@Override
	public void doRevive()
	{
		super.doRevive();
		startCastTask();
	}
	
	private final void startCastTask()
	{
		if (_majorHeal != null && _castTask == null && !isDead()) // cast task is not yet started and not dead (will start on revive)
			_castTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new CastTask(this), 3000, 1000);
	}
	
	private final void stopCastTask()
	{
		if (_castTask != null)
		{
			_castTask.cancel(false);
			_castTask = null;
		}
	}
	
	protected void castSkill(L2Skill skill)
	{
		// casting automatically stops any other action (such as autofollow or a move-to).
		// We need to gather the necessary info to restore the previous state.
		final boolean previousFollowStatus = getFollowStatus();
		
		// pet not following and owner outside cast range
		if (!previousFollowStatus && !isInsideRadius(getOwner(), skill.getCastRange(), true, true))
			return;
		
		setTarget(getOwner());
		useMagic(skill, false, false);
		
		getOwner().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1).addSkillName(skill));
		
		// calling useMagic changes the follow status, if the babypet actually casts
		// (as opposed to failing due some factors, such as too low MP, etc).
		// if the status has actually been changed, revert it. Else, allow the pet to
		// continue whatever it was trying to do.
		// NOTE: This is important since the pet may have been told to attack a target.
		// reverting the follow status will abort this attack! While aborting the attack
		// in order to heal is natural, it is not acceptable to abort the attack on its own,
		// merely because the timer stroke and without taking any other action...
		if (previousFollowStatus != getFollowStatus())
			setFollowStatus(previousFollowStatus);
	}
	
	private class CastTask implements Runnable
	{
		private final L2BabyPetInstance _baby;
		
		public CastTask(L2BabyPetInstance baby)
		{
			_baby = baby;
		}
		
		@Override
		public void run()
		{
			L2PcInstance owner = _baby.getOwner();
			
			// if the owner is dead, merely wait for the owner to be resurrected
			// if the pet is still casting from the previous iteration, allow the cast to complete...
			if (owner != null && !owner.isDead() && !owner.isInvul() && !_baby.isCastingNow() && !_baby.isBetrayed() && !_baby.isMuted() && !_baby.isOutOfControl() && _baby.getAI().getIntention() != CtrlIntention.CAST)
			{
				L2Skill skill = null;
				
				if (_majorHeal != null)
				{
					final double hpPercent = owner.getCurrentHp() / owner.getMaxHp();
					if (hpPercent < 0.15)
					{
						skill = _majorHeal.getSkill();
						if (!_baby.isSkillDisabled(skill) && Rnd.get(100) <= 75)
						{
							if (_baby.getCurrentMp() >= skill.getMpConsume())
							{
								castSkill(skill);
								return;
							}
						}
					}
					else if ((_majorHeal.getSkill() != _minorHeal.getSkill()) && hpPercent < 0.8)
					{
						// Cast _minorHeal only if it's different than _majorHeal, then pet has two heals available.
						skill = _minorHeal.getSkill();
						if (!_baby.isSkillDisabled(skill) && Rnd.get(100) <= 25)
						{
							if (_baby.getCurrentMp() >= skill.getMpConsume())
							{
								castSkill(skill);
								return;
							}
						}
					}
				}
			}
		}
	}
}