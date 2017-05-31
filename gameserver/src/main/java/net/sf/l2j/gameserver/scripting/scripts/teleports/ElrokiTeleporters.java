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
package net.sf.l2j.gameserver.scripting.scripts.teleports;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;

public class ElrokiTeleporters extends Quest
{
	public ElrokiTeleporters()
	{
		super(-1, "teleports");
		
		addStartNpc(32111, 32112);
		addTalkId(32111, 32112);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		if (npc.getNpcId() == 32111)
		{
			if (player.isInCombat())
				return "32111-no.htm";
			
			player.teleToLocation(4990, -1879, -3178, 0);
		}
		else
			player.teleToLocation(7557, -5513, -3221, 0);
		
		return null;
	}
}