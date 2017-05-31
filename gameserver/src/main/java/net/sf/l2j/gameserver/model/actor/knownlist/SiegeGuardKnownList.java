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

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;
import net.sf.l2j.gameserver.model.entity.Castle;

public class SiegeGuardKnownList extends AttackableKnownList
{
	public SiegeGuardKnownList(L2SiegeGuardInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
			return false;
		
		// get siege guard
		final L2SiegeGuardInstance guard = (L2SiegeGuardInstance) _activeObject;
		
		// Check if siege is in progress
		final Castle castle = guard.getCastle();
		if (castle != null && castle.getZone().isActive())
		{
			// get player
			final L2PcInstance player = object.getActingPlayer();
			
			// check player's clan is in siege attacker list
			if (player != null && (player.getClan() == null || castle.getSiege().getAttackerClan(player.getClan()) != null))
			{
				// try to set AI to attack
				if (guard.getAI().getIntention() == CtrlIntention.IDLE)
					guard.getAI().setIntention(CtrlIntention.ACTIVE, null);
			}
		}
		return true;
	}
}