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

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcSay;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

import java.util.StringTokenizer;

/**
 * @author Kerberos
 */
public class L2CastleTeleporterInstance extends L2NpcInstance {
    protected boolean _currentTask;
    private int _delay;

    public L2CastleTeleporterInstance(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        StringTokenizer st = new StringTokenizer(command, " ");
        String actualCommand = st.nextToken(); // Get actual command

        if (actualCommand.equalsIgnoreCase("tele")) {
            if (!_currentTask) {
                if (getCastle().getSiege().isInProgress()) {
                    if (getCastle().getSiege().getControlTowerCount() == 0) { _delay = 480000; }
                    else { _delay = 30000; }
                }
                else { _delay = 0; }

                _currentTask = true;
                ThreadPoolManager.getInstance().schedule(new oustAllPlayers(), _delay);
            }

            final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile("data/html/castleteleporter/MassGK-1.htm");
            html.replace("%delay%", getDelayInSeconds());
            player.sendPacket(html);
        }
        else { super.onBypassFeedback(player, command); }
    }

    @Override
    public void showChatWindow(L2PcInstance player) {
        String filename;
        if (!_currentTask) {
            if (getCastle().getSiege().isInProgress() && getCastle().getSiege().getControlTowerCount() == 0) {
                filename = "data/html/castleteleporter/MassGK-2.htm";
            }
            else { filename = "data/html/castleteleporter/MassGK.htm"; }
        }
        else { filename = "data/html/castleteleporter/MassGK-1.htm"; }

        final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", getObjectId());
        html.replace("%delay%", getDelayInSeconds());
        player.sendPacket(html);
    }

    private final int getDelayInSeconds() {
        return (_delay > 0) ? _delay / 1000 : 0;
    }

    protected class oustAllPlayers implements Runnable {
        @Override
        public void run() {
            // Make the region talk only during a siege
            if (getCastle().getSiege().isInProgress()) {
                final NpcSay cs = new NpcSay(getObjectId(), EChatType.SHOUT, getNpcId(), "The defenders of " + getCastle().getName() + " castle have been teleported to the inner castle.");
                final int region = MapRegionTable.getMapRegion(getX(), getY());

                for (L2PcInstance player : L2World.getInstance().getPlayers()) {
                    if (region == MapRegionTable.getMapRegion(player.getX(), player.getY())) { player.sendPacket(cs); }
                }
            }
            getCastle().oustAllPlayers();
            _currentTask = false;
        }
    }
}