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

import java.util.List;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.type.EWeaponType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Pdam implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.PDAM,
		L2SkillType.FATAL
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
			return;
		
		int damage = 0;
		
		final boolean ss = activeChar.isChargedShot(ShotType.SOULSHOT);
		
		final L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
		
		for (L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
				continue;
			
			final L2Character target = ((L2Character) obj);
			if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && ((L2PcInstance) target).isFakeDeath())
				target.stopFakeDeath(true);
			else if (target.isDead())
				continue;
			
			// Calculate skill evasion. As Dodge blocks only melee skills, make an exception with bow weapons.
			if (weapon != null && weapon.getItemType() != EWeaponType.BOW && Formulas.calcPhysicalSkillEvasion(target, skill))
			{
				if (activeChar instanceof L2PcInstance)
					((L2PcInstance) activeChar).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));
				
				if (target instanceof L2PcInstance)
					((L2PcInstance) target).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(activeChar));
				
				// no futher calculations needed.
				continue;
			}
			
			final byte shld = Formulas.calcShldUse(activeChar, target, null);
			
			// PDAM critical chance not affected by buffs, only by STR. Only some skills are meant to crit.
			boolean crit = false;
			if (skill.getBaseCritRate() > 0)
				crit = Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(activeChar));
			
			if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				damage = 0;
			else
				damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, false, ss);
			
			if (crit)
				damage *= 2; // PDAM Critical damage always 2x and not affected by buffs
				
			final byte reflect = Formulas.calcSkillReflect(target, skill);
			
			if (skill.hasEffects())
			{
				List<L2Effect> effects;
				if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
				{
					activeChar.stopSkillEffects(skill.getId());
					effects = skill.getEffects(target, activeChar);
					if (effects != null && !effects.isEmpty())
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
				}
				else
				{
					// activate attacked effects, if any
					target.stopSkillEffects(skill.getId());
					effects = skill.getEffects(activeChar, target, new Env(shld, false, false, false));
					if (effects != null && !effects.isEmpty())
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
				}
			}
			
			if (damage > 0)
			{
				activeChar.sendDamageMessage(target, damage, false, crit, false);
				
				// Possibility of a lethal strike
				Formulas.calcLethalHit(activeChar, target, skill);
				
				target.reduceCurrentHp(damage, activeChar, skill);
				
				// vengeance reflected damage
				if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
				{
					if (target instanceof L2PcInstance)
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(activeChar));
					
					if (activeChar instanceof L2PcInstance)
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(target));
					
					double vegdamage = (700 * target.getPAtk(activeChar) / activeChar.getPDef(target));
					activeChar.reduceCurrentHp(vegdamage, target, skill);
				}
			}
			else
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
		}
		
		if (skill.hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();
			
			skill.getEffectsSelf(activeChar);
		}
		
		if (skill.isSuicideAttack())
			activeChar.doDie(null);
		
		activeChar.setChargedShot(ShotType.SOULSHOT, skill.isStaticReuse());
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}