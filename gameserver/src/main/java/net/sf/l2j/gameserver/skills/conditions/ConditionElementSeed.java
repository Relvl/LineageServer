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
package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.effects.EffectSeed;

/**
 * @author Advi
 */
public class ConditionElementSeed extends Condition
{
	private static int[] SEED_SKILLS =
	{
		1285,
		1286,
		1287
	};
	private final int[] _requiredSeeds;
	
	public ConditionElementSeed(int[] seeds)
	{
		_requiredSeeds = seeds;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		int[] Seeds = new int[3];
		for (int i = 0; i < Seeds.length; i++)
		{
			Seeds[i] = (env.getCharacter().getFirstEffect(SEED_SKILLS[i]) instanceof EffectSeed ? ((EffectSeed) env.getCharacter().getFirstEffect(SEED_SKILLS[i])).getPower() : 0);
			if (Seeds[i] >= _requiredSeeds[i])
				Seeds[i] -= _requiredSeeds[i];
			else
				return false;
		}
		
		if (_requiredSeeds[3] > 0)
		{
			int count = 0;
			for (int i = 0; i < Seeds.length && count < _requiredSeeds[3]; i++)
			{
				if (Seeds[i] > 0)
				{
					Seeds[i]--;
					count++;
				}
			}
			if (count < _requiredSeeds[3])
				return false;
		}
		
		if (_requiredSeeds[4] > 0)
		{
			int count = 0;
			for (int i = 0; i < Seeds.length && count < _requiredSeeds[4]; i++)
			{
				count += Seeds[i];
			}
			if (count < _requiredSeeds[4])
				return false;
		}
		
		return true;
	}
}