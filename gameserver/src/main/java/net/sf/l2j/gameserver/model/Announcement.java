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
package net.sf.l2j.gameserver.model;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * Model of an announcement
 * @see net.sf.l2j.gameserver.datatables.AnnouncementTable
 * @author Sikken, Tryskell
 */
public class Announcement implements Runnable
{
	protected final String _message;
	
	protected boolean _critical = false;
	protected boolean _auto = false;
	protected boolean _unlimited = false;
	
	protected int _initialDelay;
	protected int _delay;
	protected int _limit;
	protected int _tempLimit; // Temporary limit, used by current timer.
	
	protected ScheduledFuture<?> _task = null;
	
	public Announcement(String message, boolean critical)
	{
		_message = message;
		_critical = critical;
	}
	
	public Announcement(String message, boolean critical, boolean auto, int initialDelay, int delay, int limit)
	{
		_message = message;
		_critical = critical;
		_auto = auto;
		_initialDelay = initialDelay;
		_delay = delay;
		_limit = limit;
		
		if (_auto)
		{
			switch (_limit)
			{
				case 0: // unlimited
					_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, _initialDelay * 1000, _delay * 1000); // self schedule at fixed rate
					_unlimited = true;
					break;
				
				default:
					_task = ThreadPoolManager.getInstance().scheduleGeneral(this, _initialDelay * 1000); // self schedule (initial)
					_tempLimit = _limit;
					break;
			}
		}
	}
	
	@Override
	public void run()
	{
		if (!_unlimited)
		{
			if (_tempLimit == 0)
				return;
			
			_task = ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000); // self schedule (worker)
			_tempLimit--;
		}
		Broadcast.announceToOnlinePlayers(_message, _critical);
	}
	
	public String getMessage()
	{
		return _message;
	}
	
	public boolean isCritical()
	{
		return _critical;
	}
	
	public boolean isAuto()
	{
		return _auto;
	}
	
	public int getInitialDelay()
	{
		return _initialDelay;
	}
	
	public int getDelay()
	{
		return _delay;
	}
	
	public int getLimit()
	{
		return _limit;
	}
	
	public void stopTask()
	{
		if (_task != null)
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	public void reloadTask()
	{
		stopTask();
		
		if (_auto)
		{
			switch (_limit)
			{
				case 0: // unlimited
					_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, _initialDelay * 1000, _delay * 1000); // self schedule at fixed rate
					_unlimited = true;
					break;
				
				default:
					_task = ThreadPoolManager.getInstance().scheduleGeneral(this, _initialDelay * 1000); // self schedule (initial)
					_tempLimit = _limit;
					break;
			}
		}
	}
}