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
package net.sf.l2j.log;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * @author Advi
 */
public class ItemFilter implements Filter
{
	// By default, filter some common consumables
	private final String _excludeProcess = "Consume";
	private final String _excludeItemType = "Arrow, Shot, Herb";
	
	@Override
	public boolean isLoggable(LogRecord record)
	{
		if (!"item".equals(record.getLoggerName()))
			return false;
		
		if (_excludeProcess != null)
		{
			String[] messageList = record.getMessage().split(":");
			if (messageList.length < 2 || !_excludeProcess.contains(messageList[1]))
				return true;
		}
		
		if (_excludeItemType != null)
		{
			ItemInstance item = ((ItemInstance) record.getParameters()[0]);
			if (!_excludeItemType.contains(item.getItemType().toString()))
				return true;
		}
		return (_excludeProcess == null && _excludeItemType == null);
	}
}