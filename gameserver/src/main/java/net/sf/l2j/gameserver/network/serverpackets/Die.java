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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;

public class Die extends L2GameServerPacket
{
	private final int _charObjId;
	private final boolean _fake;
	
	private boolean _sweepable;
	private boolean _allowFixedRes;
	private L2Clan _clan;
	L2Character _activeChar;
	
	public Die(L2Character cha)
	{
		_activeChar = cha;
		_charObjId = cha.getObjectId();
		_fake = !cha.isDead();
		
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			_allowFixedRes = player.getAccessLevel().allowFixedRes();
			_clan = player.getClan();
			
		}
		else if (cha instanceof L2Attackable)
			_sweepable = ((L2Attackable) cha).isSpoiled();
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_fake)
			return;
		
		writeC(0x06);
		writeD(_charObjId);
		writeD(0x01); // to nearest village
		
		if (_clan != null)
		{
			L2SiegeClan siegeClan = null;
			boolean isInDefense = false;
			
			Castle castle = CastleManager.getInstance().getCastle(_activeChar);
			if (castle != null && castle.getSiege().isInProgress())
			{
				// siege in progress
				siegeClan = castle.getSiege().getAttackerClan(_clan);
				if (siegeClan == null && castle.getSiege().checkIsDefender(_clan))
					isInDefense = true;
			}
			
			writeD(_clan.hasHideout() ? 0x01 : 0x00); // to hide away
			writeD(_clan.hasCastle() || isInDefense ? 0x01 : 0x00); // to castle
			writeD(siegeClan != null && !isInDefense && !siegeClan.getFlags().isEmpty() ? 0x01 : 0x00); // to siege HQ
		}
		else
		{
			writeD(0x00); // to hide away
			writeD(0x00); // to castle
			writeD(0x00); // to siege HQ
		}
		
		writeD(_sweepable ? 0x01 : 0x00); // sweepable (blue glow)
		writeD(_allowFixedRes ? 0x01 : 0x00); // FIXED
	}
}