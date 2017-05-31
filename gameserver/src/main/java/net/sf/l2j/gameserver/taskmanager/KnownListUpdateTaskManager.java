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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Periodically updates known list of all existing {@link L2Character}.<br>
 * Special scope is used for {@link L2WorldRegion} without {@link L2PcInstance} inside.
 * @author Hasha
 */
public final class KnownListUpdateTaskManager implements Runnable
{
	// Update for NPCs is performed each FULL_UPDATE tick interval.
	private static final int FULL_UPDATE = 10;
	
	private boolean _flagForgetAdd = true;
	private int _timer = FULL_UPDATE;
	
	public static final KnownListUpdateTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected KnownListUpdateTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, Config.KNOWNLIST_UPDATE_INTERVAL, Config.KNOWNLIST_UPDATE_INTERVAL);
	}
	
	@Override
	public final void run()
	{
		// Decrease and reset full iteration timer.
		if (--_timer == 0)
			_timer = FULL_UPDATE;
		
		// When iteration timer is 1, 2, perform forget and add for NPCs.
		final boolean fullUpdate = _timer < 3;
		
		// Swap forget/add flag for this iteration.
		_flagForgetAdd = !_flagForgetAdd;
		
		// Go through all world regions.
		for (L2WorldRegion regions[] : L2World.getInstance().getWorldRegions())
		{
			for (L2WorldRegion region : regions)
			{
				// Skip inactive regions unless full update (knownlist can be still updated regardless AI active or detached).
				if (!region.isActive() && !fullUpdate)
					continue;
				
				// Go through all visible objects.
				for (L2Object object : region.getVisibleObjects().values())
				{
					// don't busy about objects lower than L2Character.
					if (!(object instanceof L2Character) || !object.isVisible())
						continue;
					
					final boolean isPlayable = object instanceof L2Playable;
					final boolean isAttackable = object instanceof L2Attackable;
					
					// When one of these conditions below is passed performs forget objects (which are beyond forget distance) or add objects from surrounding regions (which are closer than detect distance)
					// 1) object is non-attackable and non-playable (NPCs) -> each FULL_UPDATE_TIMER iterations
					// 2) object is playable (players, summons, pets) -> each iteration
					// 3) object is attackable (monsters, raids, etc) -> each iteration
					if (fullUpdate || isPlayable || isAttackable)
					{
						// One iteration performs object forget.
						if (_flagForgetAdd)
							object.getKnownList().forgetObjects();
						// The other iteration performs object add.
						else
						{
							for (L2WorldRegion surroundingRegion : region.getSurroundingRegions())
							{
								// Object is a monster and surrounding region does not contain playable, skip.
								if (isAttackable && !surroundingRegion.isActive())
									continue;
								
								for (L2Object o : surroundingRegion.getVisibleObjects().values())
								{
									if (o != object)
										object.getKnownList().addKnownObject(o);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final KnownListUpdateTaskManager _instance = new KnownListUpdateTaskManager();
	}
}