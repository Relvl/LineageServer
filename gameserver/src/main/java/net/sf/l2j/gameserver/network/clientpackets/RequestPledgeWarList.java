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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.List;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PledgeReceiveWarList;

/**
 * Format: (ch) dd
 * @author -Wooden-
 */
public final class RequestPledgeWarList extends L2GameClientPacket
{
	private int _page;
	private int _tab;
	
	@Override
	protected void readImpl()
	{
		_page = readD();
		_tab = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		final L2Clan clan = activeChar.getClan();
		if (clan == null)
			return;
		
		final List<Integer> list;
		if (_tab == 0)
			list = clan.getWarList();
		else
		{
			list = clan.getAttackerList();
			
			// The page, reaching the biggest section, should send back to 0.
			_page = Math.max(0, (_page > list.size() / 13) ? 0 : _page);
		}
		
		activeChar.sendPacket(new PledgeReceiveWarList(list, _tab, _page));
	}
}