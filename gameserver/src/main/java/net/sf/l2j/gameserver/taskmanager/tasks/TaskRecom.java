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
package net.sf.l2j.gameserver.taskmanager.tasks;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.UserInfo;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author Layane
 */
public final class TaskRecom extends ATask
{
	private static final Logger _log = Logger.getLogger(TaskRecom.class.getName());
	
	private static final String NAME = "sp_recommendations";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(NAME, TaskType.TYPE_GLOBAL_TASK, "1", "06:30:00", "", 0);
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			player.restartRecom();
			player.sendPacket(new UserInfo(player));
		}
		_log.config("Recommendation Global Task: launched.");
	}
}