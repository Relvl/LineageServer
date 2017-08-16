/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.scripting.quests;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q615_MagicalPowerOfFire_Part1 extends Quest {
    private static final String qn = "Q615_MagicalPowerOfFire_Part1";

    // NPCs
    private static final int NARAN = 31378;
    private static final int UDAN = 31379;
    private static final int ASEFA_BOX = 31559;
    private static final int EYE = 31684;

    // Items
    private static final int THIEF_KEY = 1661;
    private static final int STOLEN_RED_TOTEM = 7242;
    private static final int RED_TOTEM = 7243;
    private static final int DIVINE_STONE = 7081;

    public Q615_MagicalPowerOfFire_Part1() {
        super(615, "Magical Power of Fire - Part 1");

        setItemsIds(STOLEN_RED_TOTEM);

        addStartNpc(NARAN);
        addTalkId(NARAN, UDAN, ASEFA_BOX);

        // IDs aggro ranges to avoid, else quest is automatically failed.
        addAggroRangeEnterId(21350, 21351, 21353, 21354, 21355, 21357, 21358, 21360, 21361, 21362, 21369, 21370, 21364, 21365, 21366, 21368, 21371, 21372, 21373, 21374, 21375);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("31378-03.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.set("spawned", "0");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("31559-03.htm")) {
            // You have been discovered ; quest is failed.
            if (st.getInt("spawned") == 1) { htmltext = "31559-04.htm"; }
            // No Thief's Key in inventory.
            else if (!st.hasQuestItems(THIEF_KEY)) { htmltext = "31559-02.htm"; }
            else {
                st.set("cond", "3");
                st.playSound(ESound.ItemSound_quest_itemget);
                st.takeItems(THIEF_KEY, 1);
                st.giveItems(STOLEN_RED_TOTEM, 1);
            }
        }
        else if (event.equalsIgnoreCase("UdanEyeDespawn")) {
            npc.broadcastNpcSay("I'll be waiting for your return.");
            return null;
        }

        return htmltext;
    }

    @Override
    public String onTalk(L2Npc npc, L2PcInstance player) {
        QuestState st = player.getQuestState(qn);
        String htmltext = getNoQuestMsg();
        if (st == null) { return htmltext; }

        switch (st.getState()) {
            case QuestState.STATE_CREATED:
                htmltext = (player.getLevel() >= 74 && player.getAllianceWithVarkaKetra() <= -2) ? "31378-01.htm" : "31378-02.htm";
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case NARAN:
                        htmltext = "31378-04.htm";
                        break;

                    case UDAN:
                        if (cond == 1) {
                            htmltext = "31379-01.htm";
                            st.set("cond", "2");
                            st.playSound(ESound.ItemSound_quest_middle);
                        }
                        else if (cond == 2) {
                            if (st.getInt("spawned") == 0) { htmltext = "31379-02.htm"; }
                            else {
                                htmltext = "31379-03.htm";
                                st.set("spawned", "0");
                                st.playSound(ESound.ItemSound_quest_middle);
                            }
                        }
                        else if (cond == 3 && st.hasQuestItems(STOLEN_RED_TOTEM)) {
                            htmltext = "31379-04.htm";

                            st.takeItems(STOLEN_RED_TOTEM, 1);
                            st.giveItems(RED_TOTEM, 1);
                            st.giveItems(DIVINE_STONE, 1);

                            st.unset("spawned");
                            st.playSound(ESound.ItemSound_quest_finish);
                            st.exitQuest(true);
                        }
                        break;

                    case ASEFA_BOX:
                        if (cond == 2) { htmltext = "31559-01.htm"; }
                        else if (cond == 3) { htmltext = "31559-05.htm"; }
                        break;
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onAggro(L2Npc npc, L2PcInstance player, boolean isPet) {
        QuestState st = player.getQuestState(qn);
        if (st == null) { return null; }

        if (st.getInt("spawned") == 0 && st.getInt("cond") == 2) {
            // Put "spawned" flag to 1 to avoid to spawn another.
            st.set("spawned", "1");

            // Spawn Udan's eye.
            L2Npc udanEye = addSpawn(EYE, player, true, 10000, true);
            if (udanEye != null) {
                startQuestTimer("UdanEyeDespawn", 9000, udanEye, player, false);
                udanEye.broadcastNpcSay("You cannot escape Udan's Eye!");
                st.playSound(ESound.ItemSound_quest_giveup);
            }
        }

        return null;
    }
}