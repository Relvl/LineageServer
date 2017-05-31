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

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * @author DS
 */
public class Cancel implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.CANCEL,
		L2SkillType.MAGE_BANE,
		L2SkillType.WARRIOR_BANE
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		// Delimit min/max % success.
		final int minRate = (skill.getSkillType() == L2SkillType.CANCEL) ? 25 : 40;
		final int maxRate = (skill.getSkillType() == L2SkillType.CANCEL) ? 75 : 95;
		
		// Get skill power (which is used as baseRate).
		final double skillPower = skill.getPower();
		
		for (L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
				continue;
			
			final L2Character target = (L2Character) obj;
			if (target.isDead())
				continue;
			
			int lastCanceledSkillId = 0;
			int count = skill.getMaxNegatedEffects();
			
			// Calculate the difference of level between skill level and victim, and retrieve the vuln/prof.
			final int diffLevel = skill.getMagicLevel() - target.getLevel();
			final double skillVuln = Formulas.calcSkillVulnerability(activeChar, target, skill, skill.getSkillType());
			
			for (L2Effect effect : target.getAllEffects())
			{
				// Don't cancel null effects or toggles.
				if (effect == null || effect.getSkill().isToggle())
					continue;
				
				// Mage && Warrior Bane drop only particular stacktypes.
				switch (skill.getSkillType())
				{
					case MAGE_BANE:
						if ("casting_time_down".equalsIgnoreCase(effect.getStackType()))
							break;
						
						if ("ma_up".equalsIgnoreCase(effect.getStackType()))
							break;
						
						continue;
						
					case WARRIOR_BANE:
						if ("attack_time_down".equalsIgnoreCase(effect.getStackType()))
							break;
						
						if ("speed_up".equalsIgnoreCase(effect.getStackType()))
							break;
						
						continue;
				}
				
				// If that skill effect was already canceled, continue.
				if (effect.getSkill().getId() == lastCanceledSkillId)
					continue;
				
				// Calculate the success chance following previous variables.
				if (calcCancelSuccess(effect.getPeriod(), diffLevel, skillPower, skillVuln, minRate, maxRate))
				{
					// Stores the last canceled skill for further use.
					lastCanceledSkillId = effect.getSkill().getId();
					
					// Exit the effect.
					effect.exit();
				}
				
				// Remove 1 to the stack of buffs to remove.
				count--;
				
				// If the stack goes to 0, then break the loop.
				if (count == 0)
					break;
			}
			
			// Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, skill);
		}
		
		if (skill.hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();
			
			skill.getEffectsSelf(activeChar);
		}
		activeChar.setChargedShot(activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT) ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, skill.isStaticReuse());
	}
	
	private static boolean calcCancelSuccess(int effectPeriod, int diffLevel, double baseRate, double vuln, int minRate, int maxRate)
	{
		double rate = (2 * diffLevel + baseRate + effectPeriod / 120) * vuln;
		
		if (Config.DEVELOPER)
			_log.info("calcCancelSuccess(): diffLevel:" + diffLevel + ", baseRate:" + baseRate + ", vuln:" + vuln + ", total:" + rate);
		
		if (rate < minRate)
			rate = minRate;
		else if (rate > maxRate)
			rate = maxRate;
		
		return Rnd.get(100) < rate;
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}