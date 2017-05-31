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
import net.sf.l2j.gameserver.model.actor.L2Vehicle;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.DeleteObject;
import net.sf.l2j.gameserver.network.serverpackets.SpawnItemPoly;

public class PcKnownList extends PlayableKnownList
{
	public PcKnownList(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Add a visible L2Object to L2PcInstance _knownObjects and _knownPlayer (if necessary) and send Server-Client Packets needed to inform the L2PcInstance of its state and actions in progress.<BR>
	 * <BR>
	 * <B><U> object is a ItemInstance </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packet DropItem/SpawnItem to the L2PcInstance</li><BR>
	 * <BR>
	 * <B><U> object is a L2DoorInstance </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packets DoorInfo and DoorStatusUpdate to the L2PcInstance</li> <li>Send Server->Client packet MoveToPawn/MoveToLocation and AutoAttackStart to the L2PcInstance</li><BR>
	 * <BR>
	 * <B><U> object is a L2Npc </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packet NpcInfo to the L2PcInstance</li> <li>Send Server->Client packet MoveToPawn/MoveToLocation and AutoAttackStart to the L2PcInstance</li><BR>
	 * <BR>
	 * <B><U> object is a L2Summon </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packet NpcInfo/PetItemList (if the L2PcInstance is the owner) to the L2PcInstance</li> <li>Send Server->Client packet MoveToPawn/MoveToLocation and AutoAttackStart to the L2PcInstance</li><BR>
	 * <BR>
	 * <B><U> object is a L2PcInstance </U> :</B><BR>
	 * <BR>
	 * <li>Send Server-Client Packet CharInfo to the L2PcInstance</li> <li>If the object has a private store, Send Server-Client Packet PrivateStoreMsgSell to the L2PcInstance</li> <li>Send Server->Client packet MoveToPawn/MoveToLocation and AutoAttackStart to the L2PcInstance</li><BR>
	 * <BR>
	 * @param object The L2Object to add to _knownObjects and _knownPlayer
	 */
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
			return false;
		
		sendInfoFrom(object);
		return true;
	}
	
	/**
	 * Remove a L2Object from L2PcInstance _knownObjects and _knownPlayer (if necessary) and send Server-Client Packet DeleteObject to the L2PcInstance.<BR>
	 * <BR>
	 * @param object The L2Object to remove from _knownObjects and _knownPlayer
	 */
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
			return false;
		
		// get player
		final L2PcInstance player = (L2PcInstance) _activeObject;
		
		// send Server-Client Packet DeleteObject to the L2PcInstance
		player.sendPacket(new DeleteObject(object, (object instanceof L2PcInstance) && ((L2PcInstance) object).isSeated()));
		return true;
	}
	
	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2Vehicle)
			return 8000;
		
		return Math.max(1800, 3600 - (_knownObjects.size() * 20));
	}
	
	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		// distance to watch + 50%
		return (int) Math.round(1.5 * getDistanceToWatchObject(object));
	}
	
	public final void refreshInfos()
	{
		for (L2Object object : _knownObjects.values())
		{
			if (object instanceof L2PcInstance && ((L2PcInstance) object).inObserverMode())
				continue;
			
			sendInfoFrom(object);
		}
	}
	
	private final void sendInfoFrom(L2Object object)
	{
		// get player
		final L2PcInstance player = (L2PcInstance) _activeObject;
		
		if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
			player.sendPacket(new SpawnItemPoly(object));
		else
		{
			// send object info to player
			object.sendInfo(player);
			
			if (object instanceof L2Character)
			{
				// Update the state of the L2Character object client side by sending Server->Client packet MoveToPawn/MoveToLocation and AutoAttackStart to the L2PcInstance
				L2Character obj = (L2Character) object;
				if (obj.hasAI())
					obj.getAI().describeStateToPlayer(player);
			}
		}
	}
}