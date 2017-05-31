/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.util;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.L2GameClient;

public final class FloodProtectors
{
	public static enum Action
	{
		ROLL_DICE(Config.ROLL_DICE_TIME),
		HERO_VOICE(Config.HERO_VOICE_TIME),
		SUBCLASS(Config.SUBCLASS_TIME),
		DROP_ITEM(Config.DROP_ITEM_TIME),
		SERVER_BYPASS(Config.SERVER_BYPASS_TIME),
		MULTISELL(Config.MULTISELL_TIME),
		MANUFACTURE(Config.MANUFACTURE_TIME),
		MANOR(Config.MANOR_TIME),
		SENDMAIL(Config.SENDMAIL_TIME),
		CHARACTER_SELECT(Config.CHARACTER_SELECT_TIME),
		GLOBAL_CHAT(Config.GLOBAL_CHAT_TIME),
		TRADE_CHAT(Config.TRADE_CHAT_TIME),
		SOCIAL(Config.SOCIAL_TIME);
		
		private final int _reuseDelay;
		
		private Action(int reuseDelay)
		{
			_reuseDelay = reuseDelay;
		}
		
		public int getReuseDelay()
		{
			return _reuseDelay;
		}
		
		public static final int VALUES_LENGTH = Action.values().length;
	}
	
	/**
	 * Try to perform an action according to client FPs value. A 0 reuse delay means the action is always possible.
	 * @param client : The client to check protectors on.
	 * @param action : The action to track.
	 * @return True if the action is possible, False otherwise.
	 */
	public static boolean performAction(L2GameClient client, Action action)
	{
		final int reuseDelay = action.getReuseDelay();
		if (reuseDelay == 0)
			return true;
		
		long[] value = client.getFloodProtectors();
		
		synchronized (value)
		{
			if (value[action.ordinal()] > System.currentTimeMillis())
				return false;
			
			value[action.ordinal()] = System.currentTimeMillis() + reuseDelay;
			return true;
		}
	}
}