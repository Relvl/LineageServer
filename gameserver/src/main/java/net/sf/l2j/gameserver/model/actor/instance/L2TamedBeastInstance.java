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
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

/**
 * A tamed beast behaves a lot like a pet and has an owner. Some points :
 * <ul>
 * <li>feeding another beast to level 4 will vanish your actual tamed beast.</li>
 * <li>running out of spices will vanish your actual tamed beast. There's a 1min food check timer.</li>
 * <li>running out of the Beast Farm perimeter will vanish your tamed beast.</li>
 * <li>no need to force attack it, it's a normal monster.</li>
 * </ul>
 * This class handles the running tasks (such as skills use and feed) of the mob.
 */
public final class L2TamedBeastInstance extends L2FeedableBeastInstance
{
	private static final int MAX_DISTANCE_FROM_HOME = 13000;
	private static final int MAX_DISTANCE_FROM_OWNER = 2000;
	private static final int DURATION_CHECK_INTERVAL = 60000;
	private static final int BUFF_INTERVAL = 5000;
	
	private int _foodSkillId;
	private L2PcInstance _owner;
	private Future<?> _buffTask = null;
	private Future<?> _foodTask = null;
	
	// Messages used every minute by the tamed beast when he automatically eats food.
	protected static final String[] TAMED_TEXT =
	{
		"Refills! Yeah!",
		"I am such a gluttonous beast, it is embarrassing! Ha ha.",
		"Your cooperative feeling has been getting better and better.",
		"I will help you!",
		"The weather is really good. Wanna go for a picnic?",
		"I really like you! This is tasty...",
		"If you do not have to leave this place, then I can help you.",
		"What can I help you with?",
		"I am not here only for food!",
		"Yam, yam, yam, yam, yam!"
	};
	
	public L2TamedBeastInstance(int objectId, NpcTemplate template, L2PcInstance owner, int foodSkillId, int x, int y, int z)
	{
		super(objectId, template);
		
		disableCoreAI(true); // Make it brainless.
		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());
		
		setOwner(owner);
		setFoodType(foodSkillId);
		
		spawnMe(x, y, z);
	}
	
	public int getFoodType()
	{
		return _foodSkillId;
	}
	
	public void setFoodType(int foodItemId)
	{
		if (foodItemId > 0)
		{
			_foodSkillId = foodItemId;
			
			// Cancel the food check, if existing.
			if (_foodTask != null)
				_foodTask.cancel(true);
			
			// Start the food check.
			_foodTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FoodCheck(this), DURATION_CHECK_INTERVAL, DURATION_CHECK_INTERVAL);
		}
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		getAI().stopFollow();
		
		// Clean up AIs.
		if (_buffTask != null)
			_buffTask.cancel(true);
		
		if (_foodTask != null)
			_foodTask.cancel(true);
		
		// Clean up actual trained beast.
		if (_owner != null)
			_owner.setTrainedBeast(null);
		
		// Clean up variables.
		_buffTask = null;
		_foodTask = null;
		_owner = null;
		_foodSkillId = 0;
		return true;
	}
	
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	public void setOwner(L2PcInstance owner)
	{
		if (owner != null)
		{
			_owner = owner;
			setTitle(owner.getName());
			setShowSummonAnimation(true);
			broadcastPacket(new NpcInfo(this, owner));
			
			owner.setTrainedBeast(this);
			
			// always and automatically follow the owner.
			getAI().startFollow(_owner, 200);
			
			// Cancel the buff task, if existing.
			if (_buffTask != null)
				_buffTask.cancel(true);
			
			// Start the buff task.
			_buffTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckOwnerBuffs(this, getTemplate().getBuffSkills().size()), BUFF_INTERVAL, BUFF_INTERVAL);
		}
		// Despawn if no owner
		else
			deleteMe();
	}
	
	/**
	 * The "Home" is considered as the central tower in middle of Wild Beast Reserve.
	 * @return true or false, depending of the location.
	 */
	protected boolean isTooFarFromHome()
	{
		return !(isInsideRadius(52335, -83086, MAX_DISTANCE_FROM_HOME, true));
	}
	
	@Override
	public void deleteMe()
	{
		// Clean up AI.
		if (_buffTask != null)
			_buffTask.cancel(true);
		
		_foodTask.cancel(true);
		stopHpMpRegeneration();
		
		// Clean up actual trained beast.
		if (_owner != null)
			_owner.setTrainedBeast(null);
		
		// Clean up variables.
		setTarget(null);
		_buffTask = null;
		_foodTask = null;
		_owner = null;
		_foodSkillId = 0;
		
		// Remove the spawn.
		super.deleteMe();
	}
	
	/**
	 * Notification triggered by the owner when the owner is attacked.<br>
	 * Tamed mobs will heal/recharge or debuff the enemy according to their skills.
	 * @param attacker
	 */
	public void onOwnerGotAttacked(L2Character attacker)
	{
		// Check if the owner is no longer around. If so, despawn.
		if (_owner == null || !_owner.isOnline())
		{
			deleteMe();
			return;
		}
		
		// If the owner is too far away, stop anything else and immediately run towards the owner.
		if (!_owner.isInsideRadius(this, MAX_DISTANCE_FROM_OWNER, true, true))
		{
			getAI().startFollow(_owner, 200);
			return;
		}
		
		// If the owner is dead or if the tamed beast is currently casting a spell,do nothing.
		if (_owner.isDead() || isCastingNow())
			return;
		
		int proba = Rnd.get(3);
		
		// Heal, 33% luck.
		if (proba == 0)
		{
			// Happen only when owner's HPs < 50%
			float HPRatio = ((float) _owner.getCurrentHp()) / _owner.getMaxHp();
			if (HPRatio < 0.5)
			{
				for (L2Skill skill : getTemplate().getHealSkills())
				{
					switch (skill.getSkillType())
					{
						case HEAL:
						case HOT:
						case BALANCE_LIFE:
						case HEAL_PERCENT:
						case HEAL_STATIC:
							sitCastAndFollow(skill, _owner);
							return;
					}
				}
			}
		}
		// Debuff, 33% luck.
		else if (proba == 1)
		{
			for (L2Skill skill : getTemplate().getDebuffSkills())
			{
				// if the skill is a debuff, check if the attacker has it already
				if (attacker.getFirstEffect(skill) == null)
					sitCastAndFollow(skill, attacker);
			}
		}
		// Recharge, 33% luck.
		else if (proba == 2)
		{
			// Happen only when owner's MPs < 50%
			float MPRatio = ((float) _owner.getCurrentMp()) / _owner.getMaxMp();
			if (MPRatio < 0.5)
			{
				for (L2Skill skill : getTemplate().getHealSkills())
				{
					switch (skill.getSkillType())
					{
						case MANARECHARGE:
						case MANAHEAL_PERCENT:
							sitCastAndFollow(skill, _owner);
							return;
					}
				}
			}
		}
	}
	
	/**
	 * Prepare and cast a skill:
	 * <ul>
	 * <li>First, prepare the beast for casting, by abandoning other actions.</li>
	 * <li>Next, call doCast in order to cast the spell.</li>
	 * <li>Finally, return to auto-following the owner.</li>
	 * </ul>
	 * @param skill The skill to cast.
	 * @param target The benefactor of the skill.
	 */
	protected void sitCastAndFollow(L2Skill skill, L2Character target)
	{
		stopMove(null);
		getAI().setIntention(CtrlIntention.IDLE);
		
		setTarget(target);
		doCast(skill);
		getAI().setIntention(CtrlIntention.FOLLOW, _owner);
	}
	
	private static class FoodCheck implements Runnable
	{
		private final L2TamedBeastInstance _tamedBeast;
		
		FoodCheck(L2TamedBeastInstance tamedBeast)
		{
			_tamedBeast = tamedBeast;
		}
		
		@Override
		public void run()
		{
			// Verify first if the tamed beast is still in the good range. If not, delete it.
			if (_tamedBeast.isTooFarFromHome())
			{
				// After deletion, don't go further.
				_tamedBeast.deleteMe();
				return;
			}
			
			// Destroy the food from owner's inventory ; if none is found, delete the pet.
			if (_tamedBeast.getOwner().destroyItemByItemId("BeastMob", _tamedBeast.getFoodType(), 1, _tamedBeast, true))
			{
				_tamedBeast.broadcastPacket(new SocialAction(_tamedBeast, 2));
				_tamedBeast.broadcastPacket(new NpcSay(_tamedBeast.getObjectId(), 0, _tamedBeast.getNpcId(), TAMED_TEXT[Rnd.get(TAMED_TEXT.length)]));
			}
			else
				_tamedBeast.deleteMe();
		}
	}
	
	private class CheckOwnerBuffs implements Runnable
	{
		private final L2TamedBeastInstance _tamedBeast;
		private final int _numBuffs;
		
		CheckOwnerBuffs(L2TamedBeastInstance tamedBeast, int numBuffs)
		{
			_tamedBeast = tamedBeast;
			_numBuffs = numBuffs;
		}
		
		@Override
		public void run()
		{
			L2PcInstance owner = _tamedBeast.getOwner();
			
			// Check if the owner is no longer around. If so, despawn.
			if (owner == null || !owner.isOnline())
			{
				deleteMe();
				return;
			}
			
			// If the owner is too far away, stop anything else and immediately run towards the owner.
			if (!isInsideRadius(owner, MAX_DISTANCE_FROM_OWNER, true, true))
			{
				getAI().startFollow(owner, 200);
				return;
			}
			
			// If the owner is dead or if the tamed beast is currently casting a spell,do nothing.
			if (owner.isDead() || isCastingNow())
				return;
			
			int totalBuffsOnOwner = 0;
			int i = 0;
			int rand = Rnd.get(_numBuffs);
			L2Skill buffToGive = null;
			
			// Get this npc's skills
			for (L2Skill skill : getTemplate().getBuffSkills())
			{
				if (i == rand)
					buffToGive = skill;
				
				i++;
				
				if (owner.getFirstEffect(skill) != null)
					totalBuffsOnOwner++;
			}
			
			/*
			 * If the owner has less than 60% of available buff, cast a random buff. That buff is casted only if the player hasn't it.
			 */
			if ((_numBuffs * 2 / 3) > totalBuffsOnOwner)
				if (owner.getFirstEffect(buffToGive) == null)
					_tamedBeast.sitCastAndFollow(buffToGive, owner);
			
			getAI().setIntention(CtrlIntention.FOLLOW, _tamedBeast.getOwner());
		}
	}
}