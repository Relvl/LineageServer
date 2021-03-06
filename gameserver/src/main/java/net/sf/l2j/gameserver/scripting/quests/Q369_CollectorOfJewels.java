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

public class Q369_CollectorOfJewels extends Quest {
    private static final String qn = "Q369_CollectorOfJewels";

    // NPC
    private static final int NELL = 30376;

    // Items
    private static final int FLARE_SHARD = 5882;
    private static final int FREEZING_SHARD = 5883;

    // Reward

    // Droplist
    private static final Map<Integer, int[]> DROPLIST = new HashMap<>();

    {
        DROPLIST.put(20609, new int[]
                {
                        FLARE_SHARD,
                        630000
                });
        DROPLIST.put(20612, new int[]
                {
                        FLARE_SHARD,
                        770000
                });
        DROPLIST.put(20749, new int[]
                {
                        FLARE_SHARD,
                        850000
                });
        DROPLIST.put(20616, new int[]
                {
                        FREEZING_SHARD,
                        600000
                });
        DROPLIST.put(20619, new int[]
                {
                        FREEZING_SHARD,
                        730000
                });
        DROPLIST.put(20747, new int[]
                {
                        FREEZING_SHARD,
                        850000
                });
    }

    public Q369_CollectorOfJewels() {
        super(369, "Collector of Jewels");

        setItemsIds(FLARE_SHARD, FREEZING_SHARD);

        addStartNpc(NELL);
        addTalkId(NELL);

        for (int mob : DROPLIST.keySet()) { addKillId(mob); }
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30376-03.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("30376-07.htm")) { st.playSound(ESound.ItemSound_quest_itemget); }
        else if (event.equalsIgnoreCase("30376-08.htm")) {
            st.exitQuest(true);
            st.playSound(ESound.ItemSound_quest_finish);
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
                htmltext = (player.getLevel() < 25) ? "30376-01.htm" : "30376-02.htm";
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                int flare = st.getQuestItemsCount(FLARE_SHARD);
                int freezing = st.getQuestItemsCount(FREEZING_SHARD);

                if (cond == 1) { htmltext = "30376-04.htm"; }
                else if (cond == 2 && flare >= 50 && freezing >= 50) {
                    htmltext = "30376-05.htm";
                    st.set("cond", "3");
                    st.playSound(ESound.ItemSound_quest_middle);
                    st.takeItems(FLARE_SHARD, -1);
                    st.takeItems(FREEZING_SHARD, -1);
                    st.rewardItems(ItemConst.ADENA_ID, 12500);
                }
                else if (cond == 3) { htmltext = "30376-09.htm"; }
                else if (cond == 4 && flare >= 200 && freezing >= 200) {
                    htmltext = "30376-10.htm";
                    st.takeItems(FLARE_SHARD, -1);
                    st.takeItems(FREEZING_SHARD, -1);
                    st.rewardItems(ItemConst.ADENA_ID, 63500);
                    st.playSound(ESound.ItemSound_quest_finish);
                    st.exitQuest(true);
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STATE_STARTED);
        if (partyMember == null) { return null; }

        QuestState st = partyMember.getQuestState(qn);

        int cond = st.getInt("cond");
        int[] drop = DROPLIST.get(npc.getNpcId());

        if (cond == 1) {
            if (st.dropItems(drop[0], 1, 50, drop[1]) && st.getQuestItemsCount((drop[0] == FLARE_SHARD) ? FREEZING_SHARD : FLARE_SHARD) >= 50) { st.set("cond", "2"); }
        }
        else if (cond == 3 && st.dropItems(drop[0], 1, 200, drop[1]) && st.getQuestItemsCount((drop[0] == FLARE_SHARD) ? FREEZING_SHARD : FLARE_SHARD) >= 200) { st.set("cond", "4"); }

        return null;
    }
}