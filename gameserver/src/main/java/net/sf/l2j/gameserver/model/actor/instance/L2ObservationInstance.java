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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ItemList;

import java.util.StringTokenizer;

/**
 * @author NightMarez
 */
public final class L2ObservationInstance extends L2NpcInstance {
    public L2ObservationInstance(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.startsWith("observe")) {
            StringTokenizer st = new StringTokenizer(command);
            st.nextToken();

            int cost = Integer.parseInt(st.nextToken());
            int x = Integer.parseInt(st.nextToken());
            int y = Integer.parseInt(st.nextToken());
            int z = Integer.parseInt(st.nextToken());

            if (command.startsWith("observeSiege") && SiegeManager.getSiege(x, y, z) == null) {
                player.sendPacket(SystemMessageId.ONLY_VIEW_SIEGE);
                return;
            }

            if (player.getInventory().reduceAdena(EItemProcessPurpose.BROADCAST, cost, this, true)) {
                player.enterObserverMode(x, y, z);
                player.sendPacket(new ItemList(player, false));
            }
        }
        else { super.onBypassFeedback(player, command); }
    }

    @Override
    public String getHtmlPath(int npcId, int val) {
        String filename = "";
        if (val == 0) { filename = "" + npcId; }
        else { filename = npcId + "-" + val; }

        return "data/html/observation/" + filename + ".htm";
    }
}