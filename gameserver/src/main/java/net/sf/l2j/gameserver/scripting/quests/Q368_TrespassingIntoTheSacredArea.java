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

import java.util.HashMap;
import java.util.Map;

public class Q368_TrespassingIntoTheSacredArea extends Quest {
    private static final String qn = "Q368_TrespassingIntoTheSacredArea";

    // NPC
    private static final int RESTINA = 30926;

    // Item
    private static final int FANG = 5881;

    // Drop chances
    private static final Map<Integer, Integer> CHANCES = new HashMap<>();

    {
        CHANCES.put(20794, 500000);
        CHANCES.put(20795, 770000);
        CHANCES.put(20796, 500000);
        CHANCES.put(20797, 480000);
    }

    public Q368_TrespassingIntoTheSacredArea() {
        super(368, "Trespassing into the Sacred Area");

        setItemsIds(FANG);

        addStartNpc(RESTINA);
        addTalkId(RESTINA);

        addKillId(20794, 20795, 20796, 20797);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30926-02.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("30926-05.htm")) {
            st.playSound(ESound.ItemSound_quest_finish);
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
                htmltext = (player.getLevel() < 36) ? "30926-01a.htm" : "30926-01.htm";
                break;

            case QuestState.STATE_STARTED:
                int fangs = st.getQuestItemsCount(FANG);
                if (fangs == 0) { htmltext = "30926-03.htm"; }
                else {
                    int reward = 250 * fangs + (fangs > 10 ? 5730 : 2000);

                    htmltext = "30926-04.htm";
                    st.takeItems(5881, -1);
                    st.rewardItems(ItemConst.ADENA_ID, reward);
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STATE_STARTED);
        if (partyMember == null) { return null; }

        partyMember.getQuestState(qn).dropItems(FANG, 1, 0, CHANCES.get(npc.getNpcId()));

        return null;
    }
}