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

public class Q157_RecoverSmuggledGoods extends Quest {
    private static final String qn = "Q157_RecoverSmuggledGoods";

    // Item
    private static final int ADAMANTITE_ORE = 1024;

    // Reward
    private static final int BUCKLER = 20;

    public Q157_RecoverSmuggledGoods() {
        super(157, "Recover Smuggled Goods");

        setItemsIds(ADAMANTITE_ORE);

        addStartNpc(30005); // Wilford
        addTalkId(30005);

        addKillId(20121); // Toad
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30005-05.htm")) {
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
                htmltext = (player.getLevel() < 5) ? "30005-02.htm" : "30005-03.htm";
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                if (cond == 1) { htmltext = "30005-06.htm"; }
                else if (cond == 2) {
                    htmltext = "30005-07.htm";
                    st.takeItems(ADAMANTITE_ORE, -1);
                    st.giveItems(BUCKLER, 1);
                    st.playSound(ESound.ItemSound_quest_finish);
                    st.exitQuest(false);
                }
                break;

            case QuestState.STATE_COMPLETED:
                htmltext = getAlreadyCompletedMsg();
                break;
        }
        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        QuestState st = checkPlayerCondition(player, npc, "cond", "1");
        if (st == null) { return null; }

        if (st.dropItems(ADAMANTITE_ORE, 1, 20, 400000)) { st.set("cond", "2"); }

        return null;
    }
}