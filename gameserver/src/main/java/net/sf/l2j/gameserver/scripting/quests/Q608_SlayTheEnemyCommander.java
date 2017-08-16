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

public class Q608_SlayTheEnemyCommander extends Quest {
    private static final String qn = "Q608_SlayTheEnemyCommander";

    // Quest Items
    private static final int HEAD_OF_MOS = 7236;
    private static final int TOTEM_OF_WISDOM = 7220;
    private static final int KETRA_ALLIANCE_4 = 7214;

    public Q608_SlayTheEnemyCommander() {
        super(608, "Slay the enemy commander!");

        setItemsIds(HEAD_OF_MOS);

        addStartNpc(31370); // Kadun Zu Ketra
        addTalkId(31370);

        addKillId(25312); // Mos
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("31370-04.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("31370-07.htm")) {
            if (st.hasQuestItems(HEAD_OF_MOS)) {
                st.takeItems(HEAD_OF_MOS, -1);
                st.giveItems(TOTEM_OF_WISDOM, 1);
                st.rewardExpAndSp(10000, 0);
                st.playSound(ESound.ItemSound_quest_finish);
                st.exitQuest(true);
            }
            else {
                htmltext = "31370-06.htm";
                st.set("cond", "1");
                st.playSound(ESound.ItemSound_quest_accept);
            }
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
                if (player.getLevel() >= 75) {
                    if (player.getAllianceWithVarkaKetra() >= 4 && st.hasQuestItems(KETRA_ALLIANCE_4) && !st.hasQuestItems(TOTEM_OF_WISDOM)) { htmltext = "31370-01.htm"; }
                    else { htmltext = "31370-02.htm"; }
                }
                else { htmltext = "31370-03.htm"; }
                break;

            case QuestState.STATE_STARTED:
                htmltext = (st.hasQuestItems(HEAD_OF_MOS)) ? "31370-05.htm" : "31370-06.htm";
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        for (L2PcInstance partyMember : getPartyMembers(player, npc, "cond", "1")) {
            if (partyMember.getAllianceWithVarkaKetra() >= 4) {
                QuestState st = partyMember.getQuestState(qn);
                if (st.hasQuestItems(KETRA_ALLIANCE_4)) {
                    st.set("cond", "2");
                    st.playSound(ESound.ItemSound_quest_middle);
                    st.giveItems(HEAD_OF_MOS, 1);
                }
            }
        }

        return null;
    }
}