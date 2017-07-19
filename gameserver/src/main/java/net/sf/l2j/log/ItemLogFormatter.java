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

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;

/**
 * @author Advi
 */
public class ItemLogFormatter extends Formatter
{
	private static final String CRLF = "\r\n";
	
	@Override
	public String format(LogRecord record)
	{
		final StringBuilder sb = new StringBuilder();
		
		StringUtil.append(sb, "[", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(record.getMillis()), "] ", record.getMessage());
		
		for (Object p : record.getParameters())
		{
			if (p == null)
				continue;
			
			sb.append(", ");
			if (p instanceof L2ItemInstance)
			{
				final L2ItemInstance item = (L2ItemInstance) p;
				
				StringUtil.append(sb, "item ", item.getObjectId(), ":");
				if (item.getEnchantLevel() > 0)
					StringUtil.append(sb, "+", item.getEnchantLevel(), " ");
				
				StringUtil.append(sb, item.getItem().getName(), "(", item.getCount(), ")");
			}
			else
				sb.append(p.toString());
		}
		sb.append(CRLF);
		
		return sb.toString();
	}
}