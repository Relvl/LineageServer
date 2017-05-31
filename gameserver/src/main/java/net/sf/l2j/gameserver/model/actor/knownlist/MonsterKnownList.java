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
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class MonsterKnownList extends AttackableKnownList
{
	public MonsterKnownList(L2MonsterInstance activeChar)
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
			// get monster AI
			final L2CharacterAI ai = ((L2MonsterInstance) _activeObject).getAI();
			
			// AI exists and is idle, set active
			if (ai != null && ai.getIntention() == CtrlIntention.IDLE)
				ai.setIntention(CtrlIntention.ACTIVE, null);
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
		
		// get monster
		final L2MonsterInstance monster = (L2MonsterInstance) _activeObject;
		
		// monster has AI, inform about lost object
		if (monster.hasAI())
			monster.getAI().notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);
		
		// clear agro list
		if (monster.isVisible() && getKnownType(L2PcInstance.class).isEmpty())
			monster.clearAggroList();
		
		return true;
	}
}