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

import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * Updates all data of Olympiad nobles in db
 * @author godson
 */
public final class TaskOlympiadSave extends ATask
{
	private static final Logger _log = Logger.getLogger(TaskOlympiadSave.class.getName());
	
	private static final String NAME = "OlympiadSave";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(NAME, TaskType.TYPE_FIXED_SHEDULED, "900000", "1800000", "", 0);
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		if (Olympiad.getInstance().inCompPeriod())
		{
			Olympiad.getInstance().saveOlympiadStatus();
			_log.info("Olympiad: Data updated successfully.");
		}
	}
}