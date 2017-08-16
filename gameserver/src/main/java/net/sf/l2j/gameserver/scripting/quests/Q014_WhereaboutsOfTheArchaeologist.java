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
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q014_WhereaboutsOfTheArchaeologist extends Quest {
    private static final String qn = "Q014_WhereaboutsOfTheArchaeologist";

    // NPCs
    private static final int LIESEL = 31263;
    private static final int GHOST_OF_ADVENTURER = 31538;

    // Items
    private static final int LETTER = 7253;

    public Q014_WhereaboutsOfTheArchaeologist() {
        super(14, "Whereabouts of the Archaeologist");

        setItemsIds(LETTER);

        addStartNpc(LIESEL);
        addTalkId(LIESEL, GHOST_OF_ADVENTURER);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("31263-2.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
            st.giveItems(LETTER, 1);
        }
        else if (event.equalsIgnoreCase("31538-1.htm")) {
            st.takeItems(LETTER, 1);
            st.rewardItems(ItemConst.ADENA_ID, 113228);
            st.playSound(ESound.ItemSound_quest_finish);
            st.exitQuest(false);
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
                htmltext = (player.getLevel() < 74) ? "31263-1.htm" : "31263-0.htm";
                break;

            case QuestState.STATE_STARTED:
                switch (npc.getNpcId()) {
                    case LIESEL:
                        htmltext = "31263-2.htm";
                        break;

                    case GHOST_OF_ADVENTURER:
                        htmltext = "31538-0.htm";
                        break;
                }
                break;

            case QuestState.STATE_COMPLETED:
                htmltext = getAlreadyCompletedMsg();
                break;
        }

        return htmltext;
    }
}