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
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncMDefMod extends Func
{
	static final FuncMDefMod _fpa_instance = new FuncMDefMod();
	
	public static Func getInstance()
	{
		return _fpa_instance;
	}
	
	private FuncMDefMod()
	{
		super(Stats.MAGIC_DEFENCE, 0x20, null, null);
	}
	
	@Override
	public void calc(Env env)
	{
		if (env.getCharacter() instanceof L2PetInstance)
			return;
		
		if (env.getCharacter() instanceof L2PcInstance)
		{
			final L2PcInstance player = env.getPlayer();
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
				env.subValue(5);
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
				env.subValue(5);
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
				env.subValue(9);
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
				env.subValue(9);
			if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
				env.subValue(13);
		}
		
		env.mulValue(Formulas.MENbonus[env.getCharacter().getMEN()] * env.getCharacter().getLevelMod());
	}
}