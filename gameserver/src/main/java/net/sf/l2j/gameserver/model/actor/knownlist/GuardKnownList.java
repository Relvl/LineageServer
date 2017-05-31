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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class GuardKnownList extends AttackableKnownList
{
	public GuardKnownList(L2GuardInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
			return false;
		
		// get guard
		final L2GuardInstance guard = (L2GuardInstance) _activeObject;
		
		if (object instanceof L2PcInstance)
		{
			// Check if the object added is a L2PcInstance that owns Karma
			if (((L2PcInstance) object).getKarma() > 0)
			{
				// Set the L2GuardInstance Intention to ACTIVE
				if (guard.getAI().getIntention() == CtrlIntention.IDLE)
					guard.getAI().setIntention(CtrlIntention.ACTIVE, null);
			}
		}
		else if ((Config.GUARD_ATTACK_AGGRO_MOB && guard.isInActiveRegion()) && object instanceof L2MonsterInstance)
		{
			// Check if the object added is an aggressive L2MonsterInstance
			if (((L2MonsterInstance) object).isAggressive())
			{
				// Set the L2GuardInstance Intention to ACTIVE
				if (guard.getAI().getIntention() == CtrlIntention.IDLE)
					guard.getAI().setIntention(CtrlIntention.ACTIVE, null);
			}
		}
		return true;
	}
	
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
			return false;
		
		// get guard
		final L2GuardInstance guard = (L2GuardInstance) _activeObject;
		
		// If the _aggroList of the L2GuardInstance is empty, set to IDLE
		if (guard.gotNoTarget())
		{
			if (guard.hasAI())
				guard.getAI().setIntention(CtrlIntention.IDLE, null);
		}
		return true;
	}
}