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
package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;

public class CharKnownList extends ObjectKnownList
{
	public CharKnownList(L2Character activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
			return false;
		
		// get character
		final L2Character character = (L2Character) _activeObject;
		
		// If object is targeted by the L2Character, cancel Attack or Cast
		if (object == character.getTarget())
			character.setTarget(null);
		
		return true;
	}
	
	/**
	 * Remove all objects from known list, cancel target and inform AI.
	 */
	@Override
	public final void removeAllKnownObjects()
	{
		super.removeAllKnownObjects();
		
		// get character
		final L2Character character = (L2Character) _activeObject;
		
		// set target to null
		character.setTarget(null);
		
		// cancel AI task
		if (character.hasAI())
			character.setAI(null);
	}
}