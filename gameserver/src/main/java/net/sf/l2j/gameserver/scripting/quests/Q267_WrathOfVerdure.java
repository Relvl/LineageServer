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
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q267_WrathOfVerdure extends Quest {
    private static final String qn = "Q267_WrathOfVerdure";

    // Items
    private static final int GOBLIN_CLUB = 1335;

    // Reward
    private static final int SILVERY_LEAF = 1340;

    public Q267_WrathOfVerdure() {
        super(267, "Wrath of Verdure");

        setItemsIds(GOBLIN_CLUB);

        addStartNpc(31853); // Bremec
        addTalkId(31853);

        addKillId(20325); // Goblin
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("31853-03.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("31853-06.htm")) {
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
                if (player.getRace() != PlayerRace.Elf) { htmltext = "31853-00.htm"; }
                else if (player.getLevel() < 4) { htmltext = "31853-01.htm"; }
                else { htmltext = "31853-02.htm"; }
                break;

            case QuestState.STATE_STARTED:
                int count = st.getQuestItemsCount(GOBLIN_CLUB);
                if (count > 0) {
                    htmltext = "31853-05.htm";
                    st.takeItems(GOBLIN_CLUB, -1);
                    st.rewardItems(SILVERY_LEAF, count);
                }
                else { htmltext = "31853-04.htm"; }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        QuestState st = checkPlayerState(player, npc, QuestState.STATE_STARTED);
        if (st == null) { return null; }

        st.dropItems(GOBLIN_CLUB, 1, 0, 500000);

        return null;
    }
}