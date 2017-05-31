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
package net.sf.l2j.gameserver.skills.basefuncs;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.skills.Env;

/**
 * @author mkizub
 */
public final class LambdaCalc extends Lambda
{
	private final List<Func> _funcs;
	
	public LambdaCalc()
	{
		_funcs = new ArrayList<>();
	}
	
	@Override
	public double calc(Env env)
	{
		double saveValue = env.getValue();
		try
		{
			env.setValue(0);
			for (Func f : _funcs)
				f.calc(env);
			
			return env.getValue();
		}
		finally
		{
			env.setValue(saveValue);
		}
	}
	
	public void addFunc(Func f)
	{
		_funcs.add(f);
	}
	
	public List<Func> getFuncs()
	{
		return _funcs;
	}
}