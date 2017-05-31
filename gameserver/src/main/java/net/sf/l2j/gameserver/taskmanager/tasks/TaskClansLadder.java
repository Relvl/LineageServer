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

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;

public final class TaskClansLadder extends ATask
{
	private static final String NAME = "clans_ladder";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(NAME, TaskType.TYPE_GLOBAL_TASK, "1", "00:05:00", "", 0);
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		ClanTable.getInstance().refreshClansLadder(true);
	}
}