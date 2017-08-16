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

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q377_ExplorationOfTheGiantsCave_Part2 extends Quest {
    private static final String qn = "Q377_ExplorationOfTheGiantsCave_Part2";

    // Items
    private static final int ANCIENT_BOOK = 5955;
    private static final int DICTIONARY_INTERMEDIATE = 5892;

    private static final int[][] BOOKS =
            {
                    // science & technology -> majestic leather, leather armor of nightmare
                    {
                            5945,
                            5946,
                            5947,
                            5948,
                            5949
                    },
                    // culture -> armor of nightmare, majestic plate
                    {
                            5950,
                            5951,
                            5952,
                            5953,
                            5954
                    }
            };

    // Rewards
    private static final int[][] RECIPES =
            {
                    // science & technology -> majestic leather, leather armor of nightmare
                    {
                            5338,
                            5336
                    },
                    // culture -> armor of nightmare, majestic plate
                    {
                            5420,
                            5422
                    }
            };

    public Q377_ExplorationOfTheGiantsCave_Part2() {
        super(377, "Exploration of the Giants' Cave, Part 2");

        addStartNpc(31147); // Sobling
        addTalkId(31147);

        addKillId(20654, 20656, 20657, 20658);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("31147-03.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("31147-04.htm")) {
            htmltext = checkItems(st);
        }
        else if (event.equalsIgnoreCase("31147-07.htm")) {
            st.playSound(ESound.ItemSound_quest_finish);
            st.exitQuest(true);
        }

        return htmltext;
    }

    @Override
    public String onTalk(L2Npc npc, L2PcInstance player) {
        String htmltext = getNoQuestMsg();
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        switch (st.getState()) {
            case QuestState.STATE_CREATED:
                htmltext = (player.getLevel() < 57 || !st.hasQuestItems(DICTIONARY_INTERMEDIATE)) ? "31147-01.htm" : "31147-02.htm";
                break;

            case QuestState.STATE_STARTED:
                htmltext = checkItems(st);
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STATE_STARTED);
        if (partyMember == null) { return null; }

        partyMember.getQuestState(qn).dropItems(ANCIENT_BOOK, 1, 0, 18000);

        return null;
    }

    private static String checkItems(QuestState st) {
        for (int type = 0; type < BOOKS.length; type++) {
            boolean complete = true;
            for (int book : BOOKS[type]) {
                if (!st.hasQuestItems(book)) { complete = false; }
            }

            if (complete) {
                for (int book : BOOKS[type]) { st.takeItems(book, 1); }

                st.giveItems(RECIPES[type][Rnd.get(RECIPES[type].length)], 1);
                return "31147-04.htm";
            }
        }
        return "31147-05.htm";
    }
}