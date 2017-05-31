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
package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;

public class SummonMinions extends AbstractNpcAI
{
	private static final String[] ORCS_WORDS =
	{
		"Come out, you children of darkness!",
		"Destroy the enemy, my brothers!",
		"Show yourselves!",
		"Forces of darkness! Follow me!"
	};
	
	private static final Map<Integer, int[]> MINIONS = new HashMap<>();
	{
		MINIONS.put(20767, new int[]
		{
			20768,
			20769,
			20770
		}); // Timak Orc Troop
		MINIONS.put(21524, new int[]
		{
			21525
		}); // Blade of Splendor
		MINIONS.put(21531, new int[]
		{
			21658
		}); // Punishment of Splendor
		MINIONS.put(21539, new int[]
		{
			21540
		}); // Wailing of Splendor
	}
	
	public SummonMinions()
	{
		super("ai/group");
		
		registerMobs(MINIONS.keySet(), EventType.ON_ATTACK, EventType.ON_KILL);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (npc.isScriptValue(0))
		{
			int npcId = npc.getNpcId();
			if (npcId != 20767)
			{
				for (int val : MINIONS.get(npcId))
				{
					L2Attackable newNpc = (L2Attackable) addSpawn(val, npc, true, 0, false);
					attack(newNpc, attacker);
				}
			}
			else
			{
				for (int val : MINIONS.get(npcId))
					addSpawn(val, npc, true, 0, false);
				
				npc.broadcastNpcSay(ORCS_WORDS[Rnd.get(ORCS_WORDS.length)]);
			}
			npc.setScriptValue(1);
		}
		
		return super.onAttack(npc, attacker, damage, isPet);
	}
}