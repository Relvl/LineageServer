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
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

import java.util.HashMap;
import java.util.Map;

public class Q628_HuntOfTheGoldenRamMercenaryForce extends Quest {
    private static final String qn = "Q628_HuntOfTheGoldenRamMercenaryForce";

    // NPCs
    private static final int KAHMAN = 31554;

    // Items
    private static final int SPLINTER_STAKATO_CHITIN = 7248;
    private static final int NEEDLE_STAKATO_CHITIN = 7249;
    private static final int GOLDEN_RAM_BADGE_RECRUIT = 7246;
    private static final int GOLDEN_RAM_BADGE_SOLDIER = 7247;

    // Drop chances
    private static final Map<Integer, Integer> CHANCES = new HashMap<>();

    {
        CHANCES.put(21508, 500000);
        CHANCES.put(21509, 430000);
        CHANCES.put(21510, 521000);
        CHANCES.put(21511, 575000);
        CHANCES.put(21512, 746000);
        CHANCES.put(21513, 500000);
        CHANCES.put(21514, 430000);
        CHANCES.put(21515, 520000);
        CHANCES.put(21516, 531000);
        CHANCES.put(21517, 744000);
    }

    public Q628_HuntOfTheGoldenRamMercenaryForce() {
        super(628, "Hunt of the Golden Ram Mercenary Force");

        setItemsIds(SPLINTER_STAKATO_CHITIN, NEEDLE_STAKATO_CHITIN, GOLDEN_RAM_BADGE_RECRUIT, GOLDEN_RAM_BADGE_SOLDIER);

        addStartNpc(KAHMAN);
        addTalkId(KAHMAN);

        for (int npcId : CHANCES.keySet()) { addKillId(npcId); }
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("31554-02.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("31554-03a.htm")) {
            if (st.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100 && st.getInt("cond") == 1) // Giving GOLDEN_RAM_BADGE_RECRUIT Medals
            {
                htmltext = "31554-04.htm";
                st.set("cond", "2");
                st.playSound(ESound.ItemSound_quest_middle);
                st.takeItems(SPLINTER_STAKATO_CHITIN, -1);
                st.giveItems(GOLDEN_RAM_BADGE_RECRUIT, 1);
            }
        }
        else if (event.equalsIgnoreCase("31554-07.htm")) // Cancel Quest
        {
            st.playSound(ESound.ItemSound_quest_giveup);
            st.exitQuest(true);
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
                htmltext = (player.getLevel() < 66) ? "31554-01a.htm" : "31554-01.htm";
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                if (cond == 1) {
                    if (st.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100) { htmltext = "31554-03.htm"; }
                    else { htmltext = "31554-03a.htm"; }
                }
                else if (cond == 2) {
                    if (st.getQuestItemsCount(SPLINTER_STAKATO_CHITIN) >= 100 && st.getQuestItemsCount(NEEDLE_STAKATO_CHITIN) >= 100) {
                        htmltext = "31554-05.htm";
                        st.set("cond", "3");
                        st.playSound(ESound.ItemSound_quest_finish);
                        st.takeItems(SPLINTER_STAKATO_CHITIN, -1);
                        st.takeItems(NEEDLE_STAKATO_CHITIN, -1);
                        st.takeItems(GOLDEN_RAM_BADGE_RECRUIT, 1);
                        st.giveItems(GOLDEN_RAM_BADGE_SOLDIER, 1);
                    }
                    else if (!st.hasQuestItems(SPLINTER_STAKATO_CHITIN) && !st.hasQuestItems(NEEDLE_STAKATO_CHITIN)) { htmltext = "31554-04b.htm"; }
                    else { htmltext = "31554-04a.htm"; }
                }
                else if (cond == 3) { htmltext = "31554-05a.htm"; }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STATE_STARTED);
        if (partyMember == null) { return null; }

        QuestState st = partyMember.getQuestState(qn);

        int cond = st.getInt("cond");
        int npcId = npc.getNpcId();

        switch (npcId) {
            case 21508:
            case 21509:
            case 21510:
            case 21511:
            case 21512:
                if (cond == 1 || cond == 2) { st.dropItems(SPLINTER_STAKATO_CHITIN, 1, 100, CHANCES.get(npcId)); }
                break;

            case 21513:
            case 21514:
            case 21515:
            case 21516:
            case 21517:
                if (cond == 2) { st.dropItems(NEEDLE_STAKATO_CHITIN, 1, 100, CHANCES.get(npcId)); }
                break;
        }

        return null;
    }
}