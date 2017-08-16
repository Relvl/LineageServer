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

public class Q160_NerupasRequest extends Quest {
    private static final String qn = "Q160_NerupasRequest";

    // Items
    private static final int SILVERY_SPIDERSILK = 1026;
    private static final int UNOREN_RECEIPT = 1027;
    private static final int CREAMEES_TICKET = 1028;
    private static final int NIGHTSHADE_LEAF = 1029;

    // Reward
    private static final int LESSER_HEALING_POTION = 1060;

    // NPCs
    private static final int NERUPA = 30370;
    private static final int UNOREN = 30147;
    private static final int CREAMEES = 30149;
    private static final int JULIA = 30152;

    public Q160_NerupasRequest() {
        super(160, "Nerupa's Request");

        setItemsIds(SILVERY_SPIDERSILK, UNOREN_RECEIPT, CREAMEES_TICKET, NIGHTSHADE_LEAF);

        addStartNpc(NERUPA);
        addTalkId(NERUPA, UNOREN, CREAMEES, JULIA);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30370-04.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
            st.giveItems(SILVERY_SPIDERSILK, 1);
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
                if (player.getRace() != PlayerRace.Elf) { htmltext = "30370-00.htm"; }
                else if (player.getLevel() < 3) { htmltext = "30370-02.htm"; }
                else { htmltext = "30370-03.htm"; }
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case NERUPA:
                        if (cond < 4) { htmltext = "30370-05.htm"; }
                        else if (cond == 4) {
                            htmltext = "30370-06.htm";
                            st.takeItems(NIGHTSHADE_LEAF, 1);
                            st.rewardItems(LESSER_HEALING_POTION, 5);
                            st.rewardExpAndSp(1000, 0);
                            st.playSound(ESound.ItemSound_quest_finish);
                            st.exitQuest(false);
                        }
                        break;

                    case UNOREN:
                        if (cond == 1) {
                            htmltext = "30147-01.htm";
                            st.set("cond", "2");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(SILVERY_SPIDERSILK, 1);
                            st.giveItems(UNOREN_RECEIPT, 1);
                        }
                        else if (cond == 2) { htmltext = "30147-02.htm"; }
                        else if (cond == 4) { htmltext = "30147-03.htm"; }
                        break;

                    case CREAMEES:
                        if (cond == 2) {
                            htmltext = "30149-01.htm";
                            st.set("cond", "3");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(UNOREN_RECEIPT, 1);
                            st.giveItems(CREAMEES_TICKET, 1);
                        }
                        else if (cond == 3) { htmltext = "30149-02.htm"; }
                        else if (cond == 4) { htmltext = "30149-03.htm"; }
                        break;

                    case JULIA:
                        if (cond == 3) {
                            htmltext = "30152-01.htm";
                            st.set("cond", "4");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(CREAMEES_TICKET, 1);
                            st.giveItems(NIGHTSHADE_LEAF, 1);
                        }
                        else if (cond == 4) { htmltext = "30152-02.htm"; }
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