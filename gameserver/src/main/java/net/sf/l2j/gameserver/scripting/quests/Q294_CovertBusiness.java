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

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q294_CovertBusiness extends Quest {
    private static final String qn = "Q294_CovertBusiness";

    // Item
    private static final int BAT_FANG = 1491;

    // Reward
    private static final int RING_OF_RACCOON = 1508;

    public Q294_CovertBusiness() {
        super(294, "Covert Business");

        setItemsIds(BAT_FANG);

        addStartNpc(30534); // Keef
        addTalkId(30534);

        addKillId(20370, 20480); // Barded Bat, Blade Bat
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30534-03.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
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
                if (player.getRace() != PlayerRace.Dwarf) { htmltext = "30534-00.htm"; }
                else if (player.getLevel() < 10) { htmltext = "30534-01.htm"; }
                else { htmltext = "30534-02.htm"; }
                break;

            case QuestState.STATE_STARTED:
                if (st.getInt("cond") == 1) { htmltext = "30534-04.htm"; }
                else {
                    htmltext = "30534-05.htm";
                    st.takeItems(BAT_FANG, -1);
                    st.giveItems(RING_OF_RACCOON, 1);
                    st.rewardExpAndSp(0, 600);
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

        int count = 1;
        int chance = Rnd.get(10);
        boolean isBarded = npc.getNpcId() == 20370;

        if (chance < 3) { count++; }
        else if (chance < ((isBarded) ? 5 : 6)) { count += 2; }
        else if (isBarded && chance < 7) { count += 3; }

        if (st.dropItemsAlways(BAT_FANG, count, 100)) { st.set("cond", "2"); }

        return null;
    }
}