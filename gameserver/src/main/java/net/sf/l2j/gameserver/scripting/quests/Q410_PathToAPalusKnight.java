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
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.network.client.game_to_client.SocialAction;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q410_PathToAPalusKnight extends Quest {
    private static final String qn = "Q410_PathToAPalusKnight";

    // Items
    private static final int PALUS_TALISMAN = 1237;
    private static final int LYCANTHROPE_SKULL = 1238;
    private static final int VIRGIL_LETTER = 1239;
    private static final int MORTE_TALISMAN = 1240;
    private static final int PREDATOR_CARAPACE = 1241;
    private static final int ARACHNID_TRACKER_SILK = 1242;
    private static final int COFFIN_OF_ETERNAL_REST = 1243;
    private static final int GAZE_OF_ABYSS = 1244;

    // NPCs
    private static final int KALINTA = 30422;
    private static final int VIRGIL = 30329;

    // Monsters
    private static final int POISON_SPIDER = 20038;
    private static final int ARACHNID_TRACKER = 20043;
    private static final int LYCANTHROPE = 20049;

    public Q410_PathToAPalusKnight() {
        super(410, "Path to a Palus Knight");

        setItemsIds(PALUS_TALISMAN, LYCANTHROPE_SKULL, VIRGIL_LETTER, MORTE_TALISMAN, PREDATOR_CARAPACE, ARACHNID_TRACKER_SILK, COFFIN_OF_ETERNAL_REST);

        addStartNpc(VIRGIL);
        addTalkId(VIRGIL, KALINTA);

        addKillId(POISON_SPIDER, ARACHNID_TRACKER, LYCANTHROPE);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30329-05.htm")) {
            if (player.getClassId() != ClassId.darkFighter) { htmltext = (player.getClassId() == ClassId.palusKnight) ? "30329-02a.htm" : "30329-03.htm"; }
            else if (player.getLevel() < 19) { htmltext = "30329-02.htm"; }
            else if (st.hasQuestItems(GAZE_OF_ABYSS)) { htmltext = "30329-04.htm"; }
        }
        else if (event.equalsIgnoreCase("30329-06.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
            st.giveItems(PALUS_TALISMAN, 1);
        }
        else if (event.equalsIgnoreCase("30329-10.htm")) {
            st.set("cond", "3");
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(LYCANTHROPE_SKULL, -1);
            st.takeItems(PALUS_TALISMAN, 1);
            st.giveItems(VIRGIL_LETTER, 1);
        }
        else if (event.equalsIgnoreCase("30422-02.htm")) {
            st.set("cond", "4");
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(VIRGIL_LETTER, 1);
            st.giveItems(MORTE_TALISMAN, 1);
        }
        else if (event.equalsIgnoreCase("30422-06.htm")) {
            st.set("cond", "6");
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(ARACHNID_TRACKER_SILK, -1);
            st.takeItems(MORTE_TALISMAN, 1);
            st.takeItems(PREDATOR_CARAPACE, -1);
            st.giveItems(COFFIN_OF_ETERNAL_REST, 1);
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
                htmltext = "30329-01.htm";
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case VIRGIL:
                        if (cond == 1) { htmltext = (!st.hasQuestItems(LYCANTHROPE_SKULL)) ? "30329-07.htm" : "30329-08.htm"; }
                        else if (cond == 2) { htmltext = "30329-09.htm"; }
                        else if (cond > 2 && cond < 6) { htmltext = "30329-12.htm"; }
                        else if (cond == 6) {
                            htmltext = "30329-11.htm";
                            st.takeItems(COFFIN_OF_ETERNAL_REST, 1);
                            st.giveItems(GAZE_OF_ABYSS, 1);
                            st.rewardExpAndSp(3200, 1500);
                            player.broadcastPacket(new SocialAction(player, 3));
                            st.playSound(ESound.ItemSound_quest_finish);
                            st.exitQuest(true);
                        }
                        break;

                    case KALINTA:
                        if (cond == 3) { htmltext = "30422-01.htm"; }
                        else if (cond == 4) {
                            if (!st.hasQuestItems(ARACHNID_TRACKER_SILK) || !st.hasQuestItems(PREDATOR_CARAPACE)) { htmltext = "30422-03.htm"; }
                            else { htmltext = "30422-04.htm"; }
                        }
                        else if (cond == 5) { htmltext = "30422-05.htm"; }
                        else if (cond == 6) { htmltext = "30422-06.htm"; }
                        break;
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        QuestState st = checkPlayerState(player, npc, QuestState.STATE_STARTED);
        if (st == null) { return null; }

        switch (npc.getNpcId()) {
            case LYCANTHROPE:
                if (st.getInt("cond") == 1 && st.dropItemsAlways(LYCANTHROPE_SKULL, 1, 13)) { st.set("cond", "2"); }
                break;

            case ARACHNID_TRACKER:
                if (st.getInt("cond") == 4 && st.dropItemsAlways(ARACHNID_TRACKER_SILK, 1, 5) && st.hasQuestItems(PREDATOR_CARAPACE)) { st.set("cond", "5"); }
                break;

            case POISON_SPIDER:
                if (st.getInt("cond") == 4 && st.dropItemsAlways(PREDATOR_CARAPACE, 1, 1) && st.getQuestItemsCount(ARACHNID_TRACKER_SILK) == 5) { st.set("cond", "5"); }
                break;
        }

        return null;
    }
}