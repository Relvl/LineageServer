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

public class Q006_StepIntoTheFuture extends Quest {
    private static final String qn = "Q006_StepIntoTheFuture";

    // NPCs
    private static final int ROXXY = 30006;
    private static final int BAULRO = 30033;
    private static final int SIR_COLLIN = 30311;

    // Items
    private static final int BAULRO_LETTER = 7571;

    // Rewards
    private static final int MARK_TRAVELER = 7570;
    private static final int SOE_GIRAN = 7559;

    public Q006_StepIntoTheFuture() {
        super(6, "Step into the Future");

        setItemsIds(BAULRO_LETTER);

        addStartNpc(ROXXY);
        addTalkId(ROXXY, BAULRO, SIR_COLLIN);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30006-03.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("30033-02.htm")) {
            st.set("cond", "2");
            st.playSound(ESound.ItemSound_quest_middle);
            st.giveItems(BAULRO_LETTER, 1);
        }
        else if (event.equalsIgnoreCase("30311-02.htm")) {
            if (st.hasQuestItems(BAULRO_LETTER)) {
                st.set("cond", "3");
                st.playSound(ESound.ItemSound_quest_middle);
                st.takeItems(BAULRO_LETTER, 1);
            }
            else { htmltext = "30311-03.htm"; }
        }
        else if (event.equalsIgnoreCase("30006-06.htm")) {
            st.giveItems(MARK_TRAVELER, 1);
            st.rewardItems(SOE_GIRAN, 1);
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
                if (player.getRace() != PlayerRace.Human || player.getLevel() < 3) { htmltext = "30006-01.htm"; }
                else { htmltext = "30006-02.htm"; }
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case ROXXY:
                        if (cond == 1 || cond == 2) { htmltext = "30006-04.htm"; }
                        else if (cond == 3) { htmltext = "30006-05.htm"; }
                        break;

                    case BAULRO:
                        if (cond == 1) { htmltext = "30033-01.htm"; }
                        else if (cond == 2) { htmltext = "30033-03.htm"; }
                        else { htmltext = "30033-04.htm"; }
                        break;

                    case SIR_COLLIN:
                        if (cond == 2) { htmltext = "30311-01.htm"; }
                        else if (cond == 3) { htmltext = "30311-03a.htm"; }
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