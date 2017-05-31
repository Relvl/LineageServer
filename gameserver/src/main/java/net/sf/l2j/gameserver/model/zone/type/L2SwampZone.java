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
package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2CastleZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;

/**
 * A zone where your speed is affected.
 * @author kerberos
 */
public class L2SwampZone extends L2CastleZoneType
{
	private int _moveBonus;
	
	public L2SwampZone(int id)
	{
		super(id);
		
		// Setup default speed reduce (in %)
		_moveBonus = -50;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("move_bonus"))
			_moveBonus = Integer.parseInt(value);
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		// Castle traps are active only during siege, or if they're activated.
		if (getCastle() != null && (!isEnabled() || !getCastle().getSiege().isInProgress()))
			return;
		
		character.setInsideZone(ZoneId.SWAMP, true);
		if (character instanceof L2PcInstance)
			((L2PcInstance) character).broadcastUserInfo();
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		// don't broadcast info if not needed
		if (character.isInsideZone(ZoneId.SWAMP))
		{
			character.setInsideZone(ZoneId.SWAMP, false);
			if (character instanceof L2PcInstance)
				((L2PcInstance) character).broadcastUserInfo();
		}
	}
	
	public int getMoveBonus()
	{
		return _moveBonus;
	}
}