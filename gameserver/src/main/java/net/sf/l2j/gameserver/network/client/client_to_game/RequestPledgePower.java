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
package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.ManagePledgePower;

public final class RequestPledgePower extends L2GameClientPacket {
    private int _rank;
    private int _action;
    private int _privs;

    @Override
    protected void readImpl() {
        _rank = readD();
        _action = readD();

        if (_action == 2) { _privs = readD(); }
        else { _privs = 0; }
    }

    @Override
    protected void runImpl() {
        final L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        final L2Clan clan = player.getClan();
        if (clan == null) { return; }

        if (_action == 2) {
            if (player.isClanLeader()) {
                if (_rank == 9) { _privs = (_privs & L2Clan.CP_CL_VIEW_WAREHOUSE) + (_privs & L2Clan.CP_CH_OPEN_DOOR) + (_privs & L2Clan.CP_CS_OPEN_DOOR); }

                player.getClan().setRankPrivs(_rank, _privs);
            }
        }
        else { player.sendPacket(new ManagePledgePower(clan, _action, _rank)); }
    }
}