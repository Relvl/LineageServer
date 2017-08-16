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
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q168_DeliverSupplies extends Quest {
    private static final String qn = "Q168_DeliverSupplies";

    // Items
    private static final int JENNA_LETTER = 1153;
    private static final int SENTRY_BLADE_1 = 1154;
    private static final int SENTRY_BLADE_2 = 1155;
    private static final int SENTRY_BLADE_3 = 1156;
    private static final int OLD_BRONZE_SWORD = 1157;

    // NPCs
    private static final int JENNA = 30349;
    private static final int ROSELYN = 30355;
    private static final int KRISTIN = 30357;
    private static final int HARANT = 30360;

    public Q168_DeliverSupplies() {
        super(168, "Deliver Supplies");

        setItemsIds(JENNA_LETTER, SENTRY_BLADE_1, SENTRY_BLADE_2, SENTRY_BLADE_3, OLD_BRONZE_SWORD);

        addStartNpc(JENNA);
        addTalkId(JENNA, ROSELYN, KRISTIN, HARANT);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30349-03.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
            st.giveItems(JENNA_LETTER, 1);
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
                if (player.getRace() != PlayerRace.DarkElf) { htmltext = "30349-00.htm"; }
                else if (player.getLevel() < 3) { htmltext = "30349-01.htm"; }
                else { htmltext = "30349-02.htm"; }
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case JENNA:
                        if (cond == 1) { htmltext = "30349-04.htm"; }
                        else if (cond == 2) {
                            htmltext = "30349-05.htm";
                            st.set("cond", "3");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(SENTRY_BLADE_1, 1);
                        }
                        else if (cond == 3) { htmltext = "30349-07.htm"; }
                        else if (cond == 4) {
                            htmltext = "30349-06.htm";
                            st.takeItems(OLD_BRONZE_SWORD, 2);
                            st.rewardItems(ItemConst.ADENA_ID, 820);
                            st.playSound(ESound.ItemSound_quest_finish);
                            st.exitQuest(false);
                        }
                        break;

                    case HARANT:
                        if (cond == 1) {
                            htmltext = "30360-01.htm";
                            st.set("cond", "2");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(JENNA_LETTER, 1);
                            st.giveItems(SENTRY_BLADE_1, 1);
                            st.giveItems(SENTRY_BLADE_2, 1);
                            st.giveItems(SENTRY_BLADE_3, 1);
                        }
                        else if (cond == 2) { htmltext = "30360-02.htm"; }
                        break;

                    case ROSELYN:
                        if (cond == 3) {
                            if (st.hasQuestItems(SENTRY_BLADE_2)) {
                                htmltext = "30355-01.htm";
                                st.takeItems(SENTRY_BLADE_2, 1);
                                st.giveItems(OLD_BRONZE_SWORD, 1);
                                if (st.getQuestItemsCount(OLD_BRONZE_SWORD) == 2) {
                                    st.set("cond", "4");
                                    st.playSound(ESound.ItemSound_quest_middle);
                                }
                            }
                            else { htmltext = "30355-02.htm"; }
                        }
                        else if (cond == 4) { htmltext = "30355-02.htm"; }
                        break;

                    case KRISTIN:
                        if (cond == 3) {
                            if (st.hasQuestItems(SENTRY_BLADE_3)) {
                                htmltext = "30357-01.htm";
                                st.takeItems(SENTRY_BLADE_3, 1);
                                st.giveItems(OLD_BRONZE_SWORD, 1);
                                if (st.getQuestItemsCount(OLD_BRONZE_SWORD) == 2) {
                                    st.set("cond", "4");
                                    st.playSound(ESound.ItemSound_quest_middle);
                                }
                            }
                            else { htmltext = "30357-02.htm"; }
                        }
                        else if (cond == 4) { htmltext = "30357-02.htm"; }
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