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
package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncHennaWIT extends Func
{
	static final FuncHennaWIT _fh_instance = new FuncHennaWIT();
	
	public static Func getInstance()
	{
		return _fh_instance;
	}
	
	private FuncHennaWIT()
	{
		super(Stats.STAT_WIT, 0x10, null, null);
	}
	
	@Override
	public void calc(Env env)
	{
		final L2PcInstance player = env.getPlayer();
		if (player != null)
			env.addValue(player.getHennaStatWIT());
	}
}