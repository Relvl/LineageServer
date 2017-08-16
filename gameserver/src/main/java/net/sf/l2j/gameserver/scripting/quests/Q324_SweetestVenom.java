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

public class Q324_SweetestVenom extends Quest {
    private static final String qn = "Q324_SweetestVenom";

    // Item
    private static final int VENOM_SAC = 1077;

    // Drop chances
    private static final Map<Integer, Integer> CHANCES = new HashMap<>();

    {
        CHANCES.put(20034, 220000);
        CHANCES.put(20038, 230000);
        CHANCES.put(20043, 250000);
    }

    public Q324_SweetestVenom() {
        super(324, "Sweetest Venom");

        setItemsIds(VENOM_SAC);

        addStartNpc(30351); // Astaron
        addTalkId(30351);

        addKillId(20034, 20038, 20043);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30351-04.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
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
                htmltext = (player.getLevel() < 18) ? "30351-02.htm" : "30351-03.htm";
                break;

            case QuestState.STATE_STARTED:
                if (st.getInt("cond") == 1) { htmltext = "30351-05.htm"; }
                else {
                    htmltext = "30351-06.htm";
                    st.takeItems(VENOM_SAC, -1);
                    st.rewardItems(ItemConst.ADENA_ID, 5810);
                    st.playSound(ESound.ItemSound_quest_finish);
                    st.exitQuest(true);
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        QuestState st = checkPlayerCondition(player, npc, "cond", "1");
        if (st == null) { return null; }

        if (st.dropItems(VENOM_SAC, 1, 10, CHANCES.get(npc.getNpcId()))) { st.set("cond", "2"); }

        return null;
    }
}