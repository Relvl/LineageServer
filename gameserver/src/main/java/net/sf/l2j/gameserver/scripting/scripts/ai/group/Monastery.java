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

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class Monastery extends AbstractNpcAI
{
	private static final int[] mobs1 =
	{
		22124,
		22125,
		22126,
		22127,
		22129
	};
	
	private static final int[] mobs2 =
	{
		22134,
		22135
	};
	
	public Monastery()
	{
		super("ai/group");
		
		registerMobs(mobs1, EventType.ON_AGGRO, EventType.ON_SPAWN, EventType.ON_SPELL_FINISHED);
		registerMobs(mobs2, EventType.ON_SKILL_SEE);
	}
	
	@Override
	public String onAggro(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		if (!npc.isInCombat())
		{
			if (player.getActiveWeaponInstance() != null)
			{
				npc.setTarget(player);
				npc.broadcastNpcSay(((player.getAppearance().isFemale()) ? "Sister " : "Brother ") + player.getName() + ", move your weapon away!");
				
				switch (npc.getNpcId())
				{
					case 22124:
					case 22126:
						npc.doCast(SkillTable.getInfo(4589, 8));
						break;
					
					default:
						attack(((L2Attackable) npc), player);
						break;
				}
			}
			else if (((L2Attackable) npc).getMostHated() == null)
				return null;
		}
		return super.onAggro(npc, player, isPet);
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (skill.getSkillType() == L2SkillType.AGGDAMAGE && targets.length != 0)
		{
			for (L2Object obj : targets)
			{
				if (obj.equals(npc))
				{
					npc.broadcastNpcSay(((caster.getAppearance().isFemale()) ? "Sister " : "Brother ") + caster.getName() + ", move your weapon away!");
					attack(((L2Attackable) npc), caster);
					break;
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		for (L2PcInstance target : npc.getKnownList().getKnownType(L2PcInstance.class))
		{
			if (!target.isDead() && PathFinding.getInstance().canSeeTarget(npc, target) && Util.checkIfInRange(npc.getAggroRange(), npc, target, true))
			{
				if (target.getActiveWeaponInstance() != null && !npc.isInCombat() && npc.getTarget() == null)
				{
					npc.setTarget(target);
					npc.broadcastNpcSay(((target.getAppearance().isFemale()) ? "Sister " : "Brother ") + target.getName() + ", move your weapon away!");
					
					switch (npc.getNpcId())
					{
						case 22124:
						case 22126:
						case 22127:
							npc.doCast(SkillTable.getInfo(4589, 8));
							break;
						
						default:
							attack(((L2Attackable) npc), target);
							break;
					}
				}
			}
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		if (skill.getId() == 4589)
			attack(((L2Attackable) npc), player);
		
		return super.onSpellFinished(npc, player, skill);
	}
}