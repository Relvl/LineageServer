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

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;
import net.sf.l2j.gameserver.util.Util;

/**
 * Chest AI implementation.
 * @author Fulminus
 */
public class Chests extends AbstractNpcAI
{
	private static final int SKILL_DELUXE_KEY = 2229;
	private static final int SKILL_BOX_KEY = 2065;
	
	private static final int[] NPC_IDS =
	{
		18265,
		18266,
		18267,
		18268,
		18269,
		18270,
		18271,
		18272,
		18273,
		18274,
		18275,
		18276,
		18277,
		18278,
		18279,
		18280,
		18281,
		18282,
		18283,
		18284,
		18285,
		18286,
		18287,
		18288,
		18289,
		18290,
		18291,
		18292,
		18293,
		18294,
		18295,
		18296,
		18297,
		18298,
		21671,
		21694,
		21717,
		21740,
		21763,
		21786,
		21801,
		21802,
		21803,
		21804,
		21805,
		21806,
		21807,
		21808,
		21809,
		21810,
		21811,
		21812,
		21813,
		21814,
		21815,
		21816,
		21817,
		21818,
		21819,
		21820,
		21821,
		21822
	};
	
	public Chests()
	{
		super("ai/group");
		
		registerMobs(NPC_IDS, EventType.ON_ATTACK, EventType.ON_SKILL_SEE);
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (npc instanceof L2ChestInstance)
		{
			// This behavior is only run when the target of skill is the passed npc.
			if (!Util.contains(targets, npc))
				return super.onSkillSee(npc, caster, skill, targets, isPet);
			
			final L2ChestInstance chest = ((L2ChestInstance) npc);
			
			// If this chest has already been interacted, no further AI decisions are needed.
			if (!chest.isInteracted())
			{
				chest.setInteracted();
				
				// If it's the first interaction, check if this is a box or mimic.
				if (Rnd.get(100) < 40)
				{
					switch (skill.getId())
					{
						case SKILL_BOX_KEY:
						case SKILL_DELUXE_KEY:
							// check the chance to open the box.
							int keyLevelNeeded = (chest.getLevel() / 10) - skill.getLevel();
							if (keyLevelNeeded < 0)
								keyLevelNeeded *= -1;
							
							// Regular keys got 60% to succeed.
							final int chance = ((skill.getId() == SKILL_BOX_KEY) ? 60 : 100) - keyLevelNeeded * 40;
							
							// Success, die with rewards.
							if (Rnd.get(100) < chance)
							{
								chest.setSpecialDrop();
								chest.doDie(caster);
							}
							// Used a key but failed to open: disappears with no rewards.
							else
								chest.deleteMe(); // TODO: replace for a better system (as chests attack once before decaying)
							break;
						
						default:
							chest.doCast(SkillTable.getInfo(4143, Math.min(10, Math.round(npc.getLevel() / 10))));
							break;
					}
				}
				// Mimic behavior : attack the caster.
				else
					attack(chest, ((isPet) ? caster.getPet() : caster));
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (npc instanceof L2ChestInstance)
		{
			final L2ChestInstance chest = ((L2ChestInstance) npc);
			
			// If this has already been interacted, no further AI decisions are needed.
			if (!chest.isInteracted())
			{
				chest.setInteracted();
				
				// If it was a box, cast a suicide type skill.
				if (Rnd.get(100) < 40)
					chest.doCast(SkillTable.getInfo(4143, Math.min(10, Math.round(npc.getLevel() / 10))));
				// Mimic behavior : attack the caster.
				else
					attack(chest, ((isPet) ? attacker.getPet() : attacker), ((damage * 100) / (chest.getLevel() + 7)));
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
}