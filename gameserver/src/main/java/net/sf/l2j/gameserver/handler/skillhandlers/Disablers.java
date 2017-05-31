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
package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.model.L2AttackableAI;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * This Handles Disabler skills
 * @author _drunk_
 */
public class Disablers implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.STUN,
		L2SkillType.ROOT,
		L2SkillType.SLEEP,
		L2SkillType.CONFUSION,
		L2SkillType.AGGDAMAGE,
		L2SkillType.AGGREDUCE,
		L2SkillType.AGGREDUCE_CHAR,
		L2SkillType.AGGREMOVE,
		L2SkillType.MUTE,
		L2SkillType.FAKE_DEATH,
		L2SkillType.NEGATE,
		L2SkillType.CANCEL_DEBUFF,
		L2SkillType.PARALYZE,
		L2SkillType.ERASE,
		L2SkillType.BETRAY
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		L2SkillType type = skill.getSkillType();
		
		final boolean ss = activeChar.isChargedShot(ShotType.SOULSHOT);
		final boolean sps = activeChar.isChargedShot(ShotType.SPIRITSHOT);
		final boolean bsps = activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
		
		for (L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
				continue;
			
			L2Character target = (L2Character) obj;
			if (target.isDead() || (target.isInvul() && !target.isParalyzed())) // bypass if target is dead or invul (excluding invul from Petrification)
				continue;
			
			byte shld = Formulas.calcShldUse(activeChar, target, skill);
			
			switch (type)
			{
				case BETRAY:
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, bsps))
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
					else
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					break;
				
				case FAKE_DEATH:
					// stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
					skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
					break;
				
				case ROOT:
				case STUN:
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, bsps))
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;
				
				case SLEEP:
				case PARALYZE: // use same as root for now
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, bsps))
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;
				
				case MUTE:
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, bsps))
					{
						// stop same type effect if available
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							if (e.getSkill().getSkillType() == type)
								e.exit();
						}
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;
				
				case CONFUSION:
					// do nothing if not on mob
					if (target instanceof L2Attackable)
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, bsps))
						{
							L2Effect[] effects = target.getAllEffects();
							for (L2Effect e : effects)
							{
								if (e.getSkill().getSkillType() == type)
									e.exit();
							}
							skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
						}
					}
					else
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					break;
				
				case AGGDAMAGE:
					if (target instanceof L2Attackable)
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
					
					skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
					break;
				
				case AGGREDUCE:
					// these skills needs to be rechecked
					if (target instanceof L2Attackable)
					{
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
						
						double aggdiff = ((L2Attackable) target).getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((L2Attackable) target).getHating(activeChar), target, skill);
						
						if (skill.getPower() > 0)
							((L2Attackable) target).reduceHate(null, (int) skill.getPower());
						else if (aggdiff > 0)
							((L2Attackable) target).reduceHate(null, (int) aggdiff);
					}
					break;
				
				case AGGREDUCE_CHAR:
					// these skills needs to be rechecked
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, bsps))
					{
						if (target instanceof L2Attackable)
						{
							L2Attackable targ = (L2Attackable) target;
							targ.stopHating(activeChar);
							if (targ.getMostHated() == null && targ.hasAI() && targ.getAI() instanceof L2AttackableAI)
							{
								((L2AttackableAI) targ.getAI()).setGlobalAggro(-25);
								targ.clearAggroList();
								targ.getAI().setIntention(CtrlIntention.ACTIVE);
								targ.setWalking();
							}
						}
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
						
						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					}
					break;
				
				case AGGREMOVE:
					// these skills needs to be rechecked
					if (target instanceof L2Attackable && !target.isRaid())
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, bsps))
						{
							if (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_UNDEAD)
							{
								if (target.isUndead())
									((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
							}
							else
								((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
							
							target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
						}
					}
					else
						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					break;
				
				case ERASE:
					// doesn't affect siege summons
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, bsps) && !(target instanceof L2SiegeSummonInstance))
					{
						final L2PcInstance summonOwner = ((L2Summon) target).getOwner();
						final L2Summon summonPet = summonOwner.getPet();
						if (summonPet != null)
						{
							summonPet.unSummon(summonOwner);
							summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
						}
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					}
					break;
				
				case CANCEL_DEBUFF:
					L2Effect[] effects = target.getAllEffects();
					
					if (effects == null || effects.length == 0)
						break;
					
					int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
					for (L2Effect e : effects)
					{
						if (e == null || !e.getSkill().isDebuff() || !e.getSkill().canBeDispeled())
							continue;
						
						e.exit();
						
						if (count > -1)
						{
							count++;
							if (count >= skill.getMaxNegatedEffects())
								break;
						}
					}
					break;
				
				case NEGATE:
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					
					// Skills with negateId (skillId)
					if (skill.getNegateId().length != 0)
					{
						for (int id : skill.getNegateId())
						{
							if (id != 0)
								target.stopSkillEffects(id);
						}
					}
					// All others negate type skills
					else
					{
						final int negateLvl = skill.getNegateLvl();
						
						for (L2Effect e : target.getAllEffects())
						{
							final L2Skill effectSkill = e.getSkill();
							for (L2SkillType skillType : skill.getNegateStats())
							{
								// If power is -1 the effect is always removed without lvl check
								if (negateLvl == -1)
								{
									if (effectSkill.getSkillType() == skillType || (effectSkill.getEffectType() != null && effectSkill.getEffectType() == skillType))
										e.exit();
								}
								// Remove the effect according to its power.
								else
								{
									if (effectSkill.getEffectType() != null && effectSkill.getEffectAbnormalLvl() >= 0)
									{
										if (effectSkill.getEffectType() == skillType && effectSkill.getEffectAbnormalLvl() <= negateLvl)
											e.exit();
									}
									else if (effectSkill.getSkillType() == skillType && effectSkill.getAbnormalLvl() <= negateLvl)
										e.exit();
								}
							}
						}
					}
					skill.getEffects(activeChar, target, new Env(shld, ss, sps, bsps));
					break;
			}
		}
		
		if (skill.hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();
			
			skill.getEffectsSelf(activeChar);
		}
		activeChar.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, skill.isStaticReuse());
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}