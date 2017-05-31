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
package net.sf.l2j.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;

/**
 * @author DS
 */
public final class OlympiadAnnouncer implements Runnable
{
	private static final int OLY_MANAGER = 31688;
	
	private final List<L2Spawn> _managers = new ArrayList<>();
	private int _currentStadium = 0;
	
	public OlympiadAnnouncer()
	{
		for (L2Spawn spawn : SpawnTable.getInstance().getSpawnTable())
		{
			if (spawn != null && spawn.getNpcId() == OLY_MANAGER)
				_managers.add(spawn);
		}
	}
	
	@Override
	public void run()
	{
		OlympiadGameTask task;
		for (int i = OlympiadGameManager.getInstance().getNumberOfStadiums(); --i >= 0; _currentStadium++)
		{
			if (_currentStadium >= OlympiadGameManager.getInstance().getNumberOfStadiums())
				_currentStadium = 0;
			
			task = OlympiadGameManager.getInstance().getOlympiadTask(_currentStadium);
			if (task != null && task.getGame() != null && task.needAnnounce())
			{
				String npcString;
				final String arenaId = String.valueOf(task.getGame().getStadiumId() + 1);
				switch (task.getGame().getType())
				{
					case NON_CLASSED:
						npcString = "Olympiad class-free individual match is going to begin in Arena " + arenaId + " in a moment.";
						break;
					
					case CLASSED:
						npcString = "Olympiad class individual match is going to begin in Arena " + arenaId + " in a moment.";
						break;
					
					default:
						continue;
				}
				
				L2Npc manager;
				for (L2Spawn spawn : _managers)
				{
					manager = spawn.getLastSpawn();
					if (manager != null)
						manager.broadcastPacket(new NpcSay(manager.getObjectId(), Say2.SHOUT, manager.getNpcId(), npcString));
				}
				break;
			}
		}
	}
}