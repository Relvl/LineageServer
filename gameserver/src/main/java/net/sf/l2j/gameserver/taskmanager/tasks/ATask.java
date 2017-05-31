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

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author Layane
 */
public abstract class ATask
{
	public enum TaskType
	{
		TYPE_NONE,
		TYPE_TIME,
		TYPE_SHEDULED,
		TYPE_FIXED_SHEDULED,
		TYPE_GLOBAL_TASK,
		TYPE_STARTUP,
		TYPE_SPECIAL
	}
	
	public ScheduledFuture<?> launchSpecial(ExecutedTask instance)
	{
		return null;
	}
	
	public abstract String getName();
	
	public void initializate()
	{
	}
	
	public abstract void onTimeElapsed(ExecutedTask task);
}
