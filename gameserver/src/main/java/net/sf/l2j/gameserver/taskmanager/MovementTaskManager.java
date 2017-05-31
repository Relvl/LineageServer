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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.model.actor.L2Character;

/**
 * Updates position of moving {@link L2Character} periodically. Task created as separate Thread with MAX_PRIORITY.
 * @author Forsaiken, Hasha
 */
public final class MovementTaskManager extends Thread
{
	protected static final Logger _log = Logger.getLogger(MovementTaskManager.class.getName());
	
	// Update the position of all moving characters each MILLIS_PER_UPDATE.
	private static final int MILLIS_PER_UPDATE = 100;
	
	private final Map<Integer, L2Character> _characters = new ConcurrentHashMap<>();
	
	public static final MovementTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected MovementTaskManager()
	{
		super("MovementTaskManager");
		super.setDaemon(true);
		super.setPriority(MAX_PRIORITY);
		super.start();
	}
	
	/**
	 * Add a {@link L2Character} to MovementTask in order to update its location every MILLIS_PER_UPDATE ms.
	 * @param cha The L2Character to add to movingObjects of GameTimeController
	 */
	public final void add(final L2Character cha)
	{
		_characters.putIfAbsent(cha.getObjectId(), cha);
	}
	
	@Override
	public final void run()
	{
		_log.info("MovementTaskManager: Started.");
		
		long time = System.currentTimeMillis();
		
		while (true)
		{
			// set next check time
			time += MILLIS_PER_UPDATE;
			
			try
			{
				// For all moving characters.
				for (Iterator<Map.Entry<Integer, L2Character>> iterator = _characters.entrySet().iterator(); iterator.hasNext();)
				{
					// Get entry of current iteration.
					Map.Entry<Integer, L2Character> entry = iterator.next();
					
					// Get character.
					L2Character character = entry.getValue();
					
					// Update character position, final position isn't reached yet.
					if (!character.updatePosition())
						continue;
					
					// Destination reached, remove from map.
					iterator.remove();
					
					// Get character AI, if AI doesn't exist, skip.
					final L2CharacterAI ai = character.getAI();
					if (ai == null)
						continue;
					
					// Inform AI about arrival.
					ThreadPoolManager.getInstance().executeAi(new Runnable()
					{
						@Override
						public final void run()
						{
							try
							{
								ai.notifyEvent(CtrlEvent.EVT_ARRIVED);
							}
							catch (final Throwable e)
							{
								_log.log(Level.WARNING, "", e);
							}
						}
					});
				}
			}
			catch (final Throwable e)
			{
				_log.log(Level.WARNING, "", e);
			}
			
			// Sleep thread till next tick.
			long sleepTime = time - System.currentTimeMillis();
			if (sleepTime > 0)
			{
				try
				{
					Thread.sleep(sleepTime);
				}
				catch (final InterruptedException e)
				{
					
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final MovementTaskManager _instance = new MovementTaskManager();
	}
}