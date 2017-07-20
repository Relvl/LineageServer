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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;

import java.util.Calendar;
import java.util.StringTokenizer;

/**
 * @author NightMarez
 */
public final class L2TeleporterInstance extends L2NpcInstance {
    private static final int COND_ALL_FALSE = 0;
    private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
    private static final int COND_OWNER = 2;
    private static final int COND_REGULAR = 3;

    public L2TeleporterInstance(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        player.sendPacket(ActionFailed.STATIC_PACKET);

        int condition = validateCondition(player);

        StringTokenizer st = new StringTokenizer(command, " ");
        String actualCommand = st.nextToken(); // Get actual command

        if (actualCommand.equalsIgnoreCase("goto")) {
            if (st.countTokens() <= 0) { return; }

            if (condition == COND_REGULAR || condition == COND_OWNER) {
                doTeleport(player, Integer.parseInt(st.nextToken()));
                return;
            }
        }
        else if (command.startsWith("Chat")) {
            Calendar cal = Calendar.getInstance();
            int val = 0;
            try {
                val = Integer.parseInt(command.substring(5));
            }
            catch (IndexOutOfBoundsException ioobe) {
            }
            catch (NumberFormatException nfe) {
            }

            if (val == 1 && cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7)) {
                showHalfPriceHtml(player);
                return;
            }
            showChatWindow(player, val);
        }
        else { super.onBypassFeedback(player, command); }
    }

    @Override
    public String getHtmlPath(int npcId, int val) {
        String filename = "";
        if (val == 0) { filename = "" + npcId; }
        else { filename = npcId + "-" + val; }

        return "data/html/teleporter/" + filename + ".htm";
    }

    private void showHalfPriceHtml(L2PcInstance player) {
        if (player == null) { return; }

        final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

        String content = HtmCache.getInstance().getHtm("data/html/teleporter/half/" + getNpcId() + ".htm");
        if (content == null) { content = HtmCache.getInstance().getHtmForce("data/html/teleporter/" + getNpcId() + "-1.htm"); }

        html.setHtml(content);
        html.replace("%objectId%", getObjectId());
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }

    @Override
    public void showChatWindow(L2PcInstance player) {
        String filename = "data/html/teleporter/castleteleporter-no.htm";

        int condition = validateCondition(player);
        if (condition == COND_REGULAR) {
            super.showChatWindow(player);
            return;
        }
        else if (condition > COND_ALL_FALSE) {
            if (condition == COND_BUSY_BECAUSE_OF_SIEGE) {
                filename = "data/html/teleporter/castleteleporter-busy.htm"; // Busy because of siege
            }
            else if (condition == COND_OWNER) // Clan owns castle
            {
                filename = getHtmlPath(getNpcId(), 0); // Owner message window
            }
        }

        final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", getObjectId());
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }

    private void doTeleport(L2PcInstance player, int val) {
        L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
        if (list != null) {
            // you cannot teleport to village that is in siege
            if (SiegeManager.getSiege(list.getLocX(), list.getLocY(), list.getLocZ()) != null) {
                player.sendPacket(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE);
                return;
            }

            if (MapRegionTable.townHasCastleInSiege(list.getLocX(), list.getLocY()) && isInsideZone(ZoneId.TOWN)) {
                player.sendPacket(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE);
                return;
            }

            if (!Config.KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0) // karma
            {
                player.sendMessage("Go away, you're not welcome here.");
                return;
            }

            if (list.getIsForNoble() && !player.isNoble()) {
                final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                html.setFile("data/html/teleporter/nobleteleporter-no.htm");
                html.replace("%objectId%", getObjectId());
                html.replace("%npcname%", getName());
                player.sendPacket(html);
                return;
            }

            if (player.isAlikeDead()) { return; }

            Calendar cal = Calendar.getInstance();
            int price = list.getPrice();

            if (!list.getIsForNoble()) {
                if (cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7)) { price /= 2; }
            }

            if (Config.ALT_GAME_FREE_TELEPORT || player.destroyItemByItemId(EItemProcessPurpose.TELEPORT, 57, price, this, true)) { player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), 20); }
        }
        else { LOGGER.warn("No teleport destination with id:{}", val); }

        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    private int validateCondition(L2PcInstance player) {
        if (CastleManager.getInstance().getCastleIndex(this) < 0) // Teleporter isn't on castle ground
        {
            return COND_REGULAR; // Regular access
        }

        if (getCastle().getSiege().isInProgress()) // Teleporter is on castle ground and siege is in progress
        {
            return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
        }

        if (player.getClan() != null && getCastle().getOwnerId() == player.getClanId()) // Teleporter is on castle ground and player is in a clan
        { return COND_OWNER; }

        return COND_ALL_FALSE;
    }
}