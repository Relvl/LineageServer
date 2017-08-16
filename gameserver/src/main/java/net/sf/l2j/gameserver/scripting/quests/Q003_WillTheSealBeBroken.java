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

public class Q003_WillTheSealBeBroken extends Quest {
    private static final String qn = "Q003_WillTheSealBeBroken";

    // Items
    private static final int ONYX_BEAST_EYE = 1081;
    private static final int TAINT_STONE = 1082;
    private static final int SUCCUBUS_BLOOD = 1083;

    // Reward
    private static final int SCROLL_ENCHANT_ARMOR_D = 956;

    public Q003_WillTheSealBeBroken() {
        super(3, "Will the Seal be Broken?");

        setItemsIds(ONYX_BEAST_EYE, TAINT_STONE, SUCCUBUS_BLOOD);

        addStartNpc(30141); // Talloth
        addTalkId(30141);

        addKillId(20031, 20041, 20046, 20048, 20052, 20057);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30141-03.htm")) {
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
                if (player.getRace() != PlayerRace.DarkElf) { htmltext = "30141-00.htm"; }
                else if (player.getLevel() < 16) { htmltext = "30141-01.htm"; }
                else { htmltext = "30141-02.htm"; }
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                if (cond == 1) { htmltext = "30141-04.htm"; }
                else if (cond == 2) {
                    htmltext = "30141-06.htm";
                    st.takeItems(ONYX_BEAST_EYE, 1);
                    st.takeItems(SUCCUBUS_BLOOD, 1);
                    st.takeItems(TAINT_STONE, 1);
                    st.giveItems(SCROLL_ENCHANT_ARMOR_D, 1);
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

        switch (npc.getNpcId()) {
            case 20031:
                if (st.dropItemsAlways(ONYX_BEAST_EYE, 1, 1) && st.hasQuestItems(TAINT_STONE, SUCCUBUS_BLOOD)) { st.set("cond", "2"); }
                break;

            case 20041:
            case 20046:
                if (st.dropItemsAlways(TAINT_STONE, 1, 1) && st.hasQuestItems(ONYX_BEAST_EYE, SUCCUBUS_BLOOD)) { st.set("cond", "2"); }
                break;

            case 20048:
            case 20052:
            case 20057:
                if (st.dropItemsAlways(SUCCUBUS_BLOOD, 1, 1) && st.hasQuestItems(ONYX_BEAST_EYE, TAINT_STONE)) { st.set("cond", "2"); }
                break;
        }

        return null;
    }
}