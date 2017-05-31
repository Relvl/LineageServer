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
package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.L2Npc;

/**
 * Handles {@link L2Npc} random social animation after specified time.
 */
public final class RandomAnimationTaskManager implements Runnable
{
	private final Map<L2Npc, Long> _characters = new ConcurrentHashMap<>();
	
	public static final RandomAnimationTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected RandomAnimationTaskManager()
	{
		// Run task each second.
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
	}
	
	/**
	 * Adds {@link L2Npc} to the RandomAnimationTask with additional interval.
	 * @param character : {@link L2Npc} to be added.
	 * @param interval : Interval in seconds, after which the decay task is triggered.
	 */
	public final void add(L2Npc character, int interval)
	{
		_characters.put(character, System.currentTimeMillis() + interval * 1000);
	}
	
	@Override
	public final void run()
	{
		// List is empty, skip.
		if (_characters.isEmpty())
			return;
		
		// Get current time.
		final long time = System.currentTimeMillis();
		
		// Loop all characters.
		for (Map.Entry<L2Npc, Long> entry : _characters.entrySet())
		{
			// Time hasn't passed yet, skip.
			if (time < entry.getValue())
				continue;
			
			final L2Npc character = entry.getKey();
			
			// Cancels timer on specific cases.
			if (character.isMob())
			{
				// Cancel further animation timers until intention is changed to ACTIVE again.
				if (character.getAI().getIntention() != CtrlIntention.ACTIVE)
				{
					_characters.remove(character);
					continue;
				}
			}
			else
			{
				if (!character.isInActiveRegion()) // NPCs in inactive region don't run this task
				{
					_characters.remove(character);
					continue;
				}
			}
			
			if (!(character.isDead() || character.isStunned() || character.isSleeping() || character.isParalyzed()))
				character.onRandomAnimation(Rnd.get(2, 3));
			
			// Renew the timer.
			final int timer = (character.isMob()) ? Rnd.get(Config.MIN_MONSTER_ANIMATION, Config.MAX_MONSTER_ANIMATION) : Rnd.get(Config.MIN_NPC_ANIMATION, Config.MAX_NPC_ANIMATION);
			add(character, timer);
		}
	}
	
	private static final class SingletonHolder
	{
		protected static final RandomAnimationTaskManager _instance = new RandomAnimationTaskManager();
	}
}