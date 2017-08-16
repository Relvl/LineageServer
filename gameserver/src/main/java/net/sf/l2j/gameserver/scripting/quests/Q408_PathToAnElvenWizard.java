/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
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

public class Q408_PathToAnElvenWizard extends Quest {
    private static final String qn = "Q408_PathToAnElvenWizard";

    // Items
    private static final int ROSELLA_LETTER = 1218;
    private static final int RED_DOWN = 1219;
    private static final int MAGICAL_POWERS_RUBY = 1220;
    private static final int PURE_AQUAMARINE = 1221;
    private static final int APPETIZING_APPLE = 1222;
    private static final int GOLD_LEAVES = 1223;
    private static final int IMMORTAL_LOVE = 1224;
    private static final int AMETHYST = 1225;
    private static final int NOBILITY_AMETHYST = 1226;
    private static final int FERTILITY_PERIDOT = 1229;
    private static final int ETERNITY_DIAMOND = 1230;
    private static final int CHARM_OF_GRAIN = 1272;
    private static final int SAP_OF_THE_MOTHER_TREE = 1273;
    private static final int LUCKY_POTPOURRI = 1274;

    // NPCs
    private static final int ROSELLA = 30414;
    private static final int GREENIS = 30157;
    private static final int THALIA = 30371;
    private static final int NORTHWIND = 30423;

    public Q408_PathToAnElvenWizard() {
        super(408, "Path to an Elven Wizard");

        setItemsIds(ROSELLA_LETTER, RED_DOWN, MAGICAL_POWERS_RUBY, PURE_AQUAMARINE, APPETIZING_APPLE, GOLD_LEAVES, IMMORTAL_LOVE, AMETHYST, NOBILITY_AMETHYST, FERTILITY_PERIDOT, CHARM_OF_GRAIN, SAP_OF_THE_MOTHER_TREE, LUCKY_POTPOURRI);

        addStartNpc(ROSELLA);
        addTalkId(ROSELLA, GREENIS, THALIA, NORTHWIND);

        addKillId(20047, 20019, 20466);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30414-06.htm")) {
            if (player.getClassId() != ClassId.elvenMage) { htmltext = (player.getClassId() == ClassId.elvenWizard) ? "30414-02a.htm" : "30414-03.htm"; }
            else if (player.getLevel() < 19) { htmltext = "30414-04.htm"; }
            else if (st.hasQuestItems(ETERNITY_DIAMOND)) { htmltext = "30414-05.htm"; }
            else {
                st.setState(QuestState.STATE_STARTED);
                st.set("cond", "1");
                st.playSound(ESound.ItemSound_quest_accept);
                st.giveItems(FERTILITY_PERIDOT, 1);
            }
        }
        else if (event.equalsIgnoreCase("30414-07.htm")) {
            if (!st.hasQuestItems(MAGICAL_POWERS_RUBY)) {
                st.playSound(ESound.ItemSound_quest_middle);
                st.giveItems(ROSELLA_LETTER, 1);
            }
            else { htmltext = "30414-10.htm"; }
        }
        else if (event.equalsIgnoreCase("30414-14.htm")) {
            if (!st.hasQuestItems(PURE_AQUAMARINE)) {
                st.playSound(ESound.ItemSound_quest_middle);
                st.giveItems(APPETIZING_APPLE, 1);
            }
            else { htmltext = "30414-13.htm"; }
        }
        else if (event.equalsIgnoreCase("30414-18.htm")) {
            if (!st.hasQuestItems(NOBILITY_AMETHYST)) {
                st.playSound(ESound.ItemSound_quest_middle);
                st.giveItems(IMMORTAL_LOVE, 1);
            }
            else { htmltext = "30414-17.htm"; }
        }
        else if (event.equalsIgnoreCase("30157-02.htm")) {
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(ROSELLA_LETTER, 1);
            st.giveItems(CHARM_OF_GRAIN, 1);
        }
        else if (event.equalsIgnoreCase("30371-02.htm")) {
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(APPETIZING_APPLE, 1);
            st.giveItems(SAP_OF_THE_MOTHER_TREE, 1);
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
                htmltext = "30414-01.htm";
                break;

            case QuestState.STATE_STARTED:
                switch (npc.getNpcId()) {
                    case ROSELLA:
                        if (st.hasQuestItems(MAGICAL_POWERS_RUBY, NOBILITY_AMETHYST, PURE_AQUAMARINE)) {
                            htmltext = "30414-24.htm";
                            st.takeItems(FERTILITY_PERIDOT, 1);
                            st.takeItems(MAGICAL_POWERS_RUBY, 1);
                            st.takeItems(NOBILITY_AMETHYST, 1);
                            st.takeItems(PURE_AQUAMARINE, 1);
                            st.giveItems(ETERNITY_DIAMOND, 1);
                            st.rewardExpAndSp(3200, 1890);
                            player.broadcastPacket(new SocialAction(player, 3));
                            st.playSound(ESound.ItemSound_quest_finish);
                            st.exitQuest(true);
                        }
                        else if (st.hasQuestItems(ROSELLA_LETTER)) { htmltext = "30414-08.htm"; }
                        else if (st.hasQuestItems(CHARM_OF_GRAIN)) {
                            if (st.getQuestItemsCount(RED_DOWN) == 5) { htmltext = "30414-25.htm"; }
                            else { htmltext = "30414-09.htm"; }
                        }
                        else if (st.hasQuestItems(APPETIZING_APPLE)) { htmltext = "30414-15.htm"; }
                        else if (st.hasQuestItems(SAP_OF_THE_MOTHER_TREE)) {
                            if (st.getQuestItemsCount(GOLD_LEAVES) == 5) { htmltext = "30414-26.htm"; }
                            else { htmltext = "30414-16.htm"; }
                        }
                        else if (st.hasQuestItems(IMMORTAL_LOVE)) { htmltext = "30414-19.htm"; }
                        else if (st.hasQuestItems(LUCKY_POTPOURRI)) {
                            if (st.getQuestItemsCount(AMETHYST) == 2) { htmltext = "30414-27.htm"; }
                            else { htmltext = "30414-20.htm"; }
                        }
                        else { htmltext = "30414-11.htm"; }
                        break;

                    case GREENIS:
                        if (st.hasQuestItems(ROSELLA_LETTER)) { htmltext = "30157-01.htm"; }
                        else if (st.getQuestItemsCount(RED_DOWN) == 5) {
                            htmltext = "30157-04.htm";
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(CHARM_OF_GRAIN, 1);
                            st.takeItems(RED_DOWN, -1);
                            st.giveItems(MAGICAL_POWERS_RUBY, 1);
                        }
                        else if (st.hasQuestItems(CHARM_OF_GRAIN)) { htmltext = "30157-03.htm"; }
                        break;

                    case THALIA:
                        if (st.hasQuestItems(APPETIZING_APPLE)) { htmltext = "30371-01.htm"; }
                        else if (st.getQuestItemsCount(GOLD_LEAVES) == 5) {
                            htmltext = "30371-04.htm";
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(GOLD_LEAVES, -1);
                            st.takeItems(SAP_OF_THE_MOTHER_TREE, 1);
                            st.giveItems(PURE_AQUAMARINE, 1);
                        }
                        else if (st.hasQuestItems(SAP_OF_THE_MOTHER_TREE)) { htmltext = "30371-03.htm"; }
                        break;

                    case NORTHWIND:
                        if (st.hasQuestItems(IMMORTAL_LOVE)) {
                            htmltext = "30423-01.htm";
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(IMMORTAL_LOVE, 1);
                            st.giveItems(LUCKY_POTPOURRI, 1);
                        }
                        else if (st.getQuestItemsCount(AMETHYST) == 2) {
                            htmltext = "30423-03.htm";
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(AMETHYST, -1);
                            st.takeItems(LUCKY_POTPOURRI, 1);
                            st.giveItems(NOBILITY_AMETHYST, 1);
                        }
                        else if (st.hasQuestItems(LUCKY_POTPOURRI)) { htmltext = "30423-02.htm"; }
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
            case 20019:
                if (st.hasQuestItems(SAP_OF_THE_MOTHER_TREE)) { st.dropItems(GOLD_LEAVES, 1, 5, 400000); }
                break;

            case 20047:
                if (st.hasQuestItems(LUCKY_POTPOURRI)) { st.dropItems(AMETHYST, 1, 2, 400000); }
                break;

            case 20466:
                if (st.hasQuestItems(CHARM_OF_GRAIN)) { st.dropItems(RED_DOWN, 1, 5, 700000); }
                break;
        }

        return null;
    }
}