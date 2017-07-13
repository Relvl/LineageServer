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

import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RiftInvaderInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.util.Util;

/**
 * Behavior for basic monsters.
 * @author Fulminus
 */
public final class L2AttackableAIScript extends Quest
{
	public L2AttackableAIScript()
	{
		super(-1, "ai/group");
		
		// register all mobs here...
		for (NpcTemplate template : NpcTable.getInstance().getAllNpcs())
		{
			try
			{
				if (L2Attackable.class.isAssignableFrom(Class.forName("net.sf.l2j.gameserver.model.actor.instance." + template.getType() + "Instance")))
				{
					template.addQuestEvent(EventType.ON_ATTACK, this);
					template.addQuestEvent(EventType.ON_KILL, this);
					template.addQuestEvent(EventType.ON_SPAWN, this);
					template.addQuestEvent(EventType.ON_SKILL_SEE, this);
					template.addQuestEvent(EventType.ON_FACTION_CALL, this);
					template.addQuestEvent(EventType.ON_AGGRO, this);
				}
			}
			catch (ClassNotFoundException ex)
			{
				_log.info("Class not found: " + template.getType() + "Instance");
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		return null;
	}
	
	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		return null;
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (caster == null)
			return null;
		
		if (!(npc instanceof L2Attackable))
			return null;
		
		L2Attackable attackable = (L2Attackable) npc;
		int skillAggroPoints = skill.getAggroPoints();
		
		if (caster.getPet() != null)
		{
			if (targets.length == 1 && Util.contains(targets, caster.getPet()))
				skillAggroPoints = 0;
		}
		
		if (skillAggroPoints > 0)
		{
			if (attackable.hasAI() && (attackable.getAI().getIntention() == EIntention.ATTACK))
			{
				L2Object npcTarget = attackable.getTarget();
				for (L2Object skillTarget : targets)
				{
					if (npcTarget == skillTarget || npc == skillTarget)
					{
						L2Character originalCaster = isPet ? caster.getPet() : caster;
						attackable.addDamageHate(originalCaster, 0, (skillAggroPoints * 150) / (attackable.getLevel() + 7));
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		if (attacker == null)
			return null;
		
		L2Character originalAttackTarget = (isPet ? attacker.getPet() : attacker);
		if (attacker.isInParty() && attacker.getParty().isInDimensionalRift())
		{
			byte riftType = attacker.getParty().getDimensionalRift().getType();
			byte riftRoom = attacker.getParty().getDimensionalRift().getCurrentRoom();
			
			if (caller instanceof L2RiftInvaderInstance && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
				return null;
		}
		
		// When a faction member calls for help, attack the caller's attacker.
		npc.getAI().notifyEvent(ECtrlEvent.EVT_AGGRESSION, originalAttackTarget, 1);
		return null;
	}
	
	@Override
	public String onAggro(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		if (player == null)
			return null;
		
		((L2Attackable) npc).addDamageHate(isPet ? player.getPet() : player, 0, 1);
		return null;
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		return null;
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (attacker != null && npc instanceof L2Attackable)
		{
			L2Attackable attackable = (L2Attackable) npc;
			L2Character originalAttacker = isPet ? attacker.getPet() : attacker;
			
			attackable.getAI().notifyEvent(ECtrlEvent.EVT_ATTACKED, originalAttacker);
			attackable.addDamageHate(originalAttacker, damage, (damage * 100) / (attackable.getLevel() + 7));
		}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if (npc instanceof L2MonsterInstance)
		{
			final L2MonsterInstance mob = (L2MonsterInstance) npc;
			if (mob.getLeader() != null)
				mob.getLeader().getMinionList().onMinionDie(mob, -1);
			
			if (mob.hasMinions())
				mob.getMinionList().onMasterDie(false);
		}
		return null;
	}
}