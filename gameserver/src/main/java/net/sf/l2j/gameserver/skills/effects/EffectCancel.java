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
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

/**
 * @author DS
 */
public class EffectCancel extends L2Effect
{
	public EffectCancel(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.CANCEL;
	}
	
	@Override
	public boolean onStart()
	{
		return cancel(getEffector(), getEffected(), this);
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
	
	private static boolean cancel(L2Character caster, L2Character target, L2Effect effect)
	{
		if (!(target instanceof L2PcInstance) || target.isDead())
			return false;
		
		final int cancelLvl = effect.getSkill().getMagicLevel();
		int count = effect.getSkill().getMaxNegatedEffects();
		
		double rate = effect.getEffectPower();
		
		// Resistance/vulnerability
		final double res = Formulas.calcSkillVulnerability(caster, target, effect.getSkill(), effect.getSkillType());
		rate *= res;
		
		L2Effect eff;
		int lastCanceledSkillId = 0;
		final L2Effect[] effects = target.getAllEffects();
		for (int i = effects.length; --i >= 0;)
		{
			eff = effects[i];
			if (eff == null)
				continue;
			
			// first pass - dances/songs only
			if (!eff.getSkill().isDance())
				continue;
			
			if (eff.getSkill().getId() == lastCanceledSkillId)
			{
				eff.exit(); // this skill already canceled
				continue;
			}
			
			if (!calcCancelSuccess(eff, cancelLvl, (int) rate))
				continue;
			
			lastCanceledSkillId = eff.getSkill().getId();
			eff.exit();
			count--;
			
			if (count == 0)
				break;
		}
		
		if (count != 0)
		{
			lastCanceledSkillId = 0;
			for (int i = effects.length; --i >= 0;)
			{
				eff = effects[i];
				if (eff == null)
					continue;
				
				// second pass - all except dances/songs
				if (eff.getSkill().isDance())
					continue;
				
				if (eff.getSkill().getId() == lastCanceledSkillId)
				{
					eff.exit(); // this skill already canceled
					continue;
				}
				
				if (!calcCancelSuccess(eff, cancelLvl, (int) rate))
					continue;
				
				lastCanceledSkillId = eff.getSkill().getId();
				eff.exit();
				count--;
				
				if (count == 0)
					break;
			}
		}
		return true;
	}
	
	private static boolean calcCancelSuccess(L2Effect effect, int cancelLvl, int baseRate)
	{
		int rate = 2 * (cancelLvl - effect.getSkill().getMagicLevel());
		rate += effect.getPeriod() / 120;
		rate += baseRate;
		
		if (rate < 25)
			rate = 25;
		else if (rate > 75)
			rate = 75;
		
		return Rnd.get(100) < rate;
	}
}