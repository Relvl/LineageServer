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

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;

/**
 * Destroys {@link L2Character} corpse after specified time.
 * @author Hasha
 */
public final class DecayTaskManager implements Runnable
{
	private final Map<L2Character, Long> _characters = new ConcurrentHashMap<>();
	
	public static final DecayTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected DecayTaskManager()
	{
		// Run task each second.
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
	}
	
	/**
	 * Adds {@link L2Character} to the DecayTask with additional interval.
	 * @param character : {@link L2Character} to be added.
	 * @param interval : Interval in seconds, after which the decay task is triggered.
	 */
	public final void add(L2Character character, int interval)
	{
		// if character is monster
		if (character instanceof L2Attackable)
		{
			final L2Attackable monster = ((L2Attackable) character);
			
			// monster is spoiled or seeded, double the corpse delay
			if (monster.getSpoilerId() != 0 || monster.isSeeded())
				interval *= 2;
		}
		
		_characters.put(character, System.currentTimeMillis() + interval * 1000);
	}
	
	/**
	 * Removes {@link L2Character} from the DecayTask.
	 * @param actor : {@link L2Character} to be removed.
	 */
	public final void cancel(L2Character actor)
	{
		_characters.remove(actor);
	}
	
	/**
	 * Removes {@link L2Attackable} from the DecayTask.
	 * @param monster : {@link L2Attackable} to be tested.
	 * @return boolean : True, when action can be applied on a corpse.
	 */
	public final boolean isCorpseActionAllowed(L2Attackable monster)
	{
		// get time and verify, if corpse exists
		Long time = _characters.get(monster);
		if (time == null)
			return false;
		
		// get corpse action interval, is half of corpse decay
		int corpseTime = monster.getTemplate().getCorpseTime() * 1000 / 2;
		
		// monster is spoiled or seeded, double the corpse action interval
		if (monster.getSpoilerId() != 0 || monster.isSeeded())
			corpseTime *= 2;
		
		// check last corpse action time
		return System.currentTimeMillis() < time - corpseTime;
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
		for (Map.Entry<L2Character, Long> entry : _characters.entrySet())
		{
			// Time hasn't passed yet, skip.
			if (time < entry.getValue())
				continue;
			
			final L2Character character = entry.getKey();
			
			// Decay character and remove task.
			character.onDecay();
			_characters.remove(character);
		}
	}
	
	private static final class SingletonHolder
	{
		protected static final DecayTaskManager _instance = new DecayTaskManager();
	}
}