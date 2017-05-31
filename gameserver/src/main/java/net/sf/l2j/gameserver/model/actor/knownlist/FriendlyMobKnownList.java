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

import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2FriendlyMobInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class FriendlyMobKnownList extends AttackableKnownList
{
	public FriendlyMobKnownList(L2FriendlyMobInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
			return false;
		
		// object is player
		if (object instanceof L2PcInstance)
		{
			// get friendly monster
			final L2FriendlyMobInstance monster = (L2FriendlyMobInstance) _activeObject;
			
			// AI is idle, set AI
			if (monster.getAI().getIntention() == CtrlIntention.IDLE)
				monster.getAI().setIntention(CtrlIntention.ACTIVE, null);
		}
		
		return true;
	}
	
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
			return false;
		
		if (!(object instanceof L2Character))
			return true;
		
		// get friendly monster
		final L2FriendlyMobInstance monster = (L2FriendlyMobInstance) _activeObject;
		
		if (monster.hasAI())
		{
			monster.getAI().notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);
			if (monster.getTarget() == (L2Character) object)
				monster.setTarget(null);
		}
		
		if (monster.isVisible() && getKnownType(L2PcInstance.class).isEmpty())
		{
			monster.clearAggroList();
			if (monster.hasAI())
				monster.getAI().setIntention(CtrlIntention.IDLE, null);
		}
		
		return true;
	}
}