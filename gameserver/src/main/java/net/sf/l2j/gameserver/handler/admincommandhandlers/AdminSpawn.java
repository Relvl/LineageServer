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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles following admin commands:<br>
 * - show_spawns = shows menu<br>
 * - spawn_index lvl = shows menu for monsters with respective level<br>
 * - spawn id = spawns monster id on target
 */
public class AdminSpawn implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
            {
                    "admin_list_spawns",
                    "admin_show_spawns",
                    "admin_spawn",
                    "admin_spawn_index",
                    "admin_npc_index",
                    "admin_spawn_once",
                    "admin_show_npcs",
                    "admin_spawnnight",
                    "admin_spawnday"
            };

    private static void spawn(L2PcInstance activeChar, String monsterId, int respawnTime, boolean permanent) {
        L2Object target = activeChar.getTarget();
        if (target == null) { target = activeChar; }

        NpcTemplate template;

        if (monsterId.matches("[0-9]*")) // First parameter was an ID number
        { template = NpcTable.getInstance().getTemplate(Integer.parseInt(monsterId)); }
        else
        // First parameter wasn't just numbers, so go by name not ID
        {
            monsterId = monsterId.replace('_', ' ');
            template = NpcTable.getInstance().getTemplateByName(monsterId);
        }

        try {
            L2Spawn spawn = new L2Spawn(template);
            spawn.setLocx(target.getX());
            spawn.setLocy(target.getY());
            spawn.setLocz(target.getZ());
            spawn.setHeading(activeChar.getHeading());
            spawn.setRespawnDelay(respawnTime);

            if (RaidBossSpawnManager.getInstance().getValidTemplate(spawn.getNpcId()) != null) {
                if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId())) {
                    activeChar.sendMessage("You cannot spawn another instance of " + template.getName() + ".");
                    return;
                }

                spawn.setRespawnMinDelay(43200);
                spawn.setRespawnMaxDelay(129600);
                RaidBossSpawnManager.getInstance().addNewSpawn(spawn, 0, 0, 0, permanent);
            }
            else {
                SpawnTable.getInstance().addNewSpawn(spawn, permanent);
                spawn.init();
            }

            if (!permanent) { spawn.stopRespawn(); }

            activeChar.sendMessage("Spawned " + template.getName() + ".");
        }
        catch (Exception e) {
            activeChar.sendPacket(SystemMessageId.APPLICANT_INFORMATION_INCORRECT);
        }
    }

    private static void showMonsters(L2PcInstance activeChar, int level, int from) {
        final List<NpcTemplate> mobs = NpcTable.getInstance().getTemplates(t -> t.isType("L2Monster") && t.getLevel() == level);
        final StringBuilder sb = new StringBuilder(200 + mobs.size() * 100);

        StringUtil.append(sb, "<html><title>Spawn Monster:</title><body><p> Level : ", level, "<br>Total Npc's : ", mobs.size(), "<br>");

        int i = from;
        for (int j = 0; i < mobs.size() && j < 50; i++, j++) { StringUtil.append(sb, "<a action=\"bypass -h admin_spawn ", mobs.get(i).getNpcId(), "\">", mobs.get(i).getName(), "</a><br1>"); }

        if (i == mobs.size()) { sb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>"); }
        else {
            StringUtil.append(sb, "<br><center><button value=\"Next\" action=\"bypass -h admin_spawn_index ", level, " ", i, "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
        }

        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(sb.toString());
        activeChar.sendPacket(html);
    }

    private static void showNpcs(L2PcInstance activeChar, String starting, int from) {
        final List<NpcTemplate> mobs = NpcTable.getInstance().getTemplates(t -> t.isType("L2Npc") && t.getName().startsWith(starting));
        final StringBuilder sb = new StringBuilder(200 + mobs.size() * 100);

        StringUtil.append(sb, "<html><title>Spawn Monster:</title><body><p> There are ", mobs.size(), " Npcs whose name starts with ", starting, ":<br>");

        int i = from;
        for (int j = 0; i < mobs.size() && j < 50; i++, j++) { StringUtil.append(sb, "<a action=\"bypass -h admin_spawn ", mobs.get(i).getNpcId(), "\">", mobs.get(i).getName(), "</a><br1>"); }

        if (i == mobs.size()) { sb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>"); }
        else {
            StringUtil.append(sb, "<br><center><button value=\"Next\" action=\"bypass -h admin_npc_index ", starting, " ", i, "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
        }

        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(sb.toString());
        activeChar.sendPacket(html);
    }

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (command.startsWith("admin_list_spawns")) {
            int npcId = 0;

            try {
                String[] params = command.split(" ");
                Pattern pattern = Pattern.compile("[0-9]*");
                Matcher regexp = pattern.matcher(params[1]);

                if (regexp.matches()) { npcId = Integer.parseInt(params[1]); }
                else {
                    params[1] = params[1].replace('_', ' ');
                    npcId = NpcTable.getInstance().getTemplateByName(params[1]).getNpcId();
                }
            }
            catch (Exception e) {
                // If the parameter wasn't ok, then take the current target.
                final L2Object target = activeChar.getTarget();
                if (target instanceof L2Npc) { npcId = ((L2Npc) target).getNpcId(); }
            }

            // Load static Htm.
            final NpcHtmlMessage html = new NpcHtmlMessage(0);
            html.setFile("data/html/admin/listspawns.htm");

            // Generate data.
            final StringBuilder sb = new StringBuilder();

            int index = 0, x, y, z;
            String name = "";

            for (L2Spawn spawn : SpawnTable.getInstance().getSpawnTable()) {
                if (npcId == spawn.getNpcId()) {
                    index++;
                    name = spawn.getTemplate().getName();

                    final L2Npc _npc = spawn.getLastSpawn();
                    if (_npc != null) {
                        x = _npc.getX();
                        y = _npc.getY();
                        z = _npc.getZ();
                    }
                    else {
                        x = spawn.getLocx();
                        y = spawn.getLocy();
                        z = spawn.getLocz();
                    }
                    StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_move_to ", x, " ", y, " ", z, "\">", index, " - (", x, " ", y, " ", z, ")", "</a></td></tr>");
                }
            }

            if (index == 0) {
                html.replace("%npcid%", "?");
                html.replace("%list%", "<tr><td>The parameter you entered as npcId is invalid.</td></tr>");
            }
            else {
                html.replace("%npcid%", name + " (" + npcId + ")");
                html.replace("%list%", sb.toString());
            }

            activeChar.sendPacket(html);
        }
        else if (command.equals("admin_show_spawns")) { AdminHelpPage.showHelpPage(activeChar, "spawns.htm"); }
        else if (command.startsWith("admin_spawn_index")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            try {
                st.nextToken();
                int level = Integer.parseInt(st.nextToken());
                int from = 0;
                try {
                    from = Integer.parseInt(st.nextToken());
                }
                catch (NoSuchElementException nsee) {
                }
                showMonsters(activeChar, level, from);
            }
            catch (Exception e) {
                AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
            }
        }
        else if (command.equals("admin_show_npcs")) { AdminHelpPage.showHelpPage(activeChar, "npcs.htm"); }
        else if (command.startsWith("admin_npc_index")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            try {
                st.nextToken();
                String letter = st.nextToken();
                int from = 0;
                try {
                    from = Integer.parseInt(st.nextToken());
                }
                catch (NoSuchElementException nsee) {
                }
                showNpcs(activeChar, letter, from);
            }
            catch (Exception e) {
                AdminHelpPage.showHelpPage(activeChar, "npcs.htm");
            }
        }
        else if (command.startsWith("admin_spawn")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            try {
                String cmd = st.nextToken();
                String id = st.nextToken();
                int respawnTime = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;

                if (cmd.equalsIgnoreCase("admin_spawn_once")) { spawn(activeChar, id, respawnTime, false); }
                else { spawn(activeChar, id, respawnTime, true); }
            }
            catch (Exception e) {
                AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
            }
        }
        else if (command.startsWith("admin_spawnday")) { DayNightSpawnManager.getInstance().spawnDayCreatures(); }
        else if (command.startsWith("admin_spawnnight")) { DayNightSpawnManager.getInstance().spawnNightCreatures(); }
        return true;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}