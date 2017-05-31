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
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncPDefMod extends Func
{
	static final FuncPDefMod _fpa_instance = new FuncPDefMod();
	
	public static Func getInstance()
	{
		return _fpa_instance;
	}
	
	private FuncPDefMod()
	{
		super(Stats.POWER_DEFENCE, 0x20, null, null);
	}
	
	@Override
	public void calc(Env env)
	{
		if (env.getCharacter() instanceof L2PcInstance)
		{
			final L2PcInstance player = env.getPlayer();
			final boolean hasMagePDef = (player.getClassId().isMage() || player.getClassId().getId() == 0x31); // orc mystics are a special case
			
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null)
				env.subValue(12);
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) != null)
				env.subValue((hasMagePDef) ? 15 : 31);
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null)
				env.subValue((hasMagePDef) ? 8 : 18);
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null)
				env.subValue(8);
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) != null)
				env.subValue(7);
		}
		
		env.mulValue(env.getCharacter().getLevelMod());
	}
}