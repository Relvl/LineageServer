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
package net.sf.l2j.gameserver.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.gameserver.model.actor.L2Npc;

public class L2SiegeClan
{
	private final List<L2Npc> _flags = new CopyOnWriteArrayList<>();
	private final int _clanId;
	
	private SiegeClanType _type;
	
	public enum SiegeClanType
	{
		OWNER,
		DEFENDER,
		ATTACKER,
		DEFENDER_PENDING
	}
	
	public L2SiegeClan(int clanId, SiegeClanType type)
	{
		_clanId = clanId;
		_type = type;
	}
	
	public void addFlag(L2Npc flag)
	{
		_flags.add(flag);
	}
	
	public boolean removeFlag(L2Npc flag)
	{
		if (flag == null)
			return false;
		
		flag.deleteMe();
		return _flags.remove(flag);
	}
	
	public void removeFlags()
	{
		for (L2Npc flag : _flags)
			flag.deleteMe();
		
		_flags.clear();
	}
	
	public List<L2Npc> getFlags()
	{
		return _flags;
	}
	
	public int getClanId()
	{
		return _clanId;
	}
	
	public SiegeClanType getType()
	{
		return _type;
	}
	
	public void setType(SiegeClanType setType)
	{
		_type = setType;
	}
}