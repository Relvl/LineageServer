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
package net.sf.l2j.gameserver.scripting;

import java.util.Calendar;
import java.util.logging.Logger;

/**
 * @author Hasha
 */
public abstract class ScheduledQuest extends Quest
{
	protected static final Logger _log = Logger.getLogger(ScheduledQuest.class.getName());
	
	public enum Schedule
	{
		DAILY,
		WEEKLY,
		MONTHLY_DAY,
		MONTHLY_WEEK,
		YEARLY_DAY,
		YEARLY_WEEK,
	}
	
	private Schedule _type;
	private Calendar _start;
	private Calendar _end;
	private boolean _started;
	
	public ScheduledQuest(int questId, String descr)
	{
		super(questId, descr);
	}
	
	/**
	 * Set up schedule system for the script. Returns true, when successfully done.
	 * @param type : Type of the schedule.
	 * @param start : Start information.
	 * @param end : End information.
	 * @return boolean : True, when successfully loaded schedule system.
	 */
	public final boolean setSchedule(String type, String start, String end)
	{
		try
		{
			_type = Enum.valueOf(Schedule.class, type);
			_start = Calendar.getInstance();
			_end = Calendar.getInstance();
			
			String[] startTimestamp;
			String[] endTimestamp;
			
			switch (_type)
			{
				case DAILY:
					// DAILY, "16:20:10", "17:20:00"
					startTimestamp = start.split(":");
					endTimestamp = end.split(":");
					break;
				
				case WEEKLY:
					// WEEKLY, "MON 6:20:10", "FRI 17:20:00"
					String[] params = start.split(" ");
					startTimestamp = params[1].split(":");
					_start.set(Calendar.DAY_OF_WEEK, getDayOfWeek(params[0]));
					
					params = end.split(" ");
					endTimestamp = params[1].split(":");
					_end.set(Calendar.DAY_OF_WEEK, getDayOfWeek(params[0]));
					break;
				
				case MONTHLY_DAY:
					// MONTHLY_DAY, "1 6:20:10", "2 17:20:00"
					params = start.split(" ");
					startTimestamp = params[1].split(":");
					_start.set(Calendar.DAY_OF_MONTH, Integer.valueOf(params[0]));
					
					params = end.split(" ");
					endTimestamp = params[1].split(":");
					_end.set(Calendar.DAY_OF_MONTH, Integer.valueOf(params[0]));
					break;
				
				case MONTHLY_WEEK:
					// MONTHLY_WEEK, "MON-1 6:20:10", "FRI-2 17:20:00"
					params = start.split(" ");
					String[] date = params[0].split("-");
					startTimestamp = params[1].split(":");
					_start.set(Calendar.DAY_OF_WEEK, getDayOfWeek(date[0]));
					_start.set(Calendar.WEEK_OF_MONTH, Integer.valueOf(date[1]));
					
					params = end.split(" ");
					date = params[0].split("-");
					endTimestamp = params[1].split(":");
					_end.set(Calendar.DAY_OF_WEEK, getDayOfWeek(date[0]));
					_end.set(Calendar.WEEK_OF_MONTH, Integer.valueOf(date[1]));
					break;
				
				case YEARLY_DAY:
					// YEARLY_DAY, "23-02 6:20:10", "25-03 17:20:00"
					params = start.split(" ");
					date = params[0].split("-");
					startTimestamp = params[1].split(":");
					_start.set(Calendar.DAY_OF_MONTH, Integer.valueOf(date[0]));
					_start.set(Calendar.MONTH, Integer.valueOf(date[1]) - 1);
					
					params = end.split(" ");
					date = params[0].split("-");
					endTimestamp = params[1].split(":");
					_end.set(Calendar.DAY_OF_MONTH, Integer.valueOf(date[0]));
					_end.set(Calendar.MONTH, Integer.valueOf(date[1]) - 1);
					break;
				
				case YEARLY_WEEK:
					// YEARLY_WEEK, "1 6:20:10", "2 17:20:00"
					params = start.split(" ");
					date = params[0].split("-");
					startTimestamp = params[1].split(":");
					_start.set(Calendar.DAY_OF_WEEK, getDayOfWeek(date[0]));
					_start.set(Calendar.WEEK_OF_YEAR, Integer.valueOf(date[1]));
					
					params = end.split(" ");
					date = params[0].split("-");
					endTimestamp = params[1].split(":");
					_end.set(Calendar.DAY_OF_WEEK, getDayOfWeek(date[0]));
					_end.set(Calendar.WEEK_OF_YEAR, Integer.valueOf(date[1]));
					break;
				
				default:
					// unknown Schedule type
					_type = null;
					_start = null;
					_end = null;
					return false;
			}
			
			// set hour, minute and second
			_start.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startTimestamp[0]));
			_start.set(Calendar.MINUTE, Integer.valueOf(startTimestamp[1]));
			_start.set(Calendar.SECOND, Integer.valueOf(startTimestamp[2]));
			_start.set(Calendar.MILLISECOND, 0);
			_end.set(Calendar.HOUR_OF_DAY, Integer.valueOf(endTimestamp[0]));
			_end.set(Calendar.MINUTE, Integer.valueOf(endTimestamp[1]));
			_end.set(Calendar.SECOND, Integer.valueOf(endTimestamp[2]));
			_end.set(Calendar.MILLISECOND, 0);
			
			// check correct data
			if (_end.getTimeInMillis() > _start.getTimeInMillis())
			{
				_log.warning(getName() + ": Start is scheduled after the end, script is not scheduled.");
				return false;
			}
			
			// schedule start/end
			final long now = System.currentTimeMillis();
			if (_end.getTimeInMillis() < now)
			{
				// last schedule had passed, schedule next start
				_started = false;
				notifyEndScheduleStart();
			}
			else if (_start.getTimeInMillis() < now)
			{
				// last schedule is running, schedule end
				_started = true;
				notifyStartScheduleEnd();
			}
			else
			{
				// last schedule has not started yet, schedule start
				_started = false;
			}
			
			// everything went fine, return
			return true;
		}
		catch (Exception e)
		{
			_log.warning(getName() + ": Error while loading schedule data.");
			_type = null;
			_start = null;
			_end = null;
			return false;
		}
	}
	
	/**
	 * Returns time left till next action of the script.
	 * @return long : Time in milliseconds.
	 */
	public final long getTimeLeft()
	{
		if (_type == null)
			return 0;
		
		return _started ? _end.getTimeInMillis() : _start.getTimeInMillis();
	}
	
	/**
	 * Notify and schedule next action of the script.
	 */
	public final void notifyAndSchedule()
	{
		if (_type == null)
			return;
		
		if (_started)
			notifyEndScheduleStart();
		else
			notifyStartScheduleEnd();
	}
	
	/**
	 * Launches onStart() method and schedule end of the script.
	 */
	private final void notifyStartScheduleEnd()
	{
		// notify start
		onStart();
		
		// schedule end
		switch (_type)
		{
			case DAILY:
				_end.add(Calendar.DAY_OF_WEEK, 1);
				break;
			
			case WEEKLY:
				_end.add(Calendar.WEEK_OF_YEAR, 1);
				break;
			
			case MONTHLY_DAY:
				_end.add(Calendar.DAY_OF_MONTH, 1);
				break;
			
			case MONTHLY_WEEK:
				_end.add(Calendar.WEEK_OF_MONTH, 1);
				break;
			
			case YEARLY_DAY:
				_end.add(Calendar.DAY_OF_YEAR, 1);
				break;
			
			case YEARLY_WEEK:
				_end.add(Calendar.WEEK_OF_YEAR, 1);
				break;
		}
	}
	
	/**
	 * Starts a script. Handles spawns, announcements, loads variables, etc...
	 */
	public abstract void onStart();
	
	/**
	 * Launches onEnd() method and schedule next start of the script.
	 */
	private final void notifyEndScheduleStart()
	{
		// notify end
		onEnd();
		
		// schedule start
		switch (_type)
		{
			case DAILY:
				_start.add(Calendar.DAY_OF_WEEK, 1);
				break;
			
			case WEEKLY:
				_start.add(Calendar.WEEK_OF_YEAR, 1);
				break;
			
			case MONTHLY_DAY:
				_start.add(Calendar.DAY_OF_MONTH, 1);
				break;
			
			case MONTHLY_WEEK:
				_start.add(Calendar.WEEK_OF_MONTH, 1);
				break;
			
			case YEARLY_DAY:
				_start.add(Calendar.DAY_OF_YEAR, 1);
				break;
			
			case YEARLY_WEEK:
				_start.add(Calendar.WEEK_OF_YEAR, 1);
				break;
		}
	}
	
	/**
	 * Starts a script. Handles spawns, announcements, saves variables, etc...
	 */
	public abstract void onEnd();
	
	/**
	 * Convert text representation of day {@link Calendar} day.
	 * @param day : String representation of day.
	 * @return int : {@link Calendar} representation of day.
	 * @throws Exception : Throws {@link Exception}, when can't convert day.
	 */
	private static final int getDayOfWeek(String day) throws Exception
	{
		if (day.equals("MON"))
			return Calendar.MONDAY;
		else if (day.equals("TUE"))
			return Calendar.TUESDAY;
		else if (day.equals("WED"))
			return Calendar.WEDNESDAY;
		else if (day.equals("THU"))
			return Calendar.THURSDAY;
		else if (day.equals("FRI"))
			return Calendar.FRIDAY;
		else if (day.equals("SAT"))
			return Calendar.SATURDAY;
		else if (day.equals("SUN"))
			return Calendar.SUNDAY;
		else
			throw new Exception();
	}
}