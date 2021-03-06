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
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q660_AidingTheFloranVillage extends Quest {
    private static final String qn = "Q660_AidingTheFloranVillage";

    // NPCs
    private static final int MARIA = 30608;
    private static final int ALEX = 30291;

    // Items
    private static final int WATCHING_EYES = 8074;
    private static final int GOLEM_SHARD = 8075;
    private static final int LIZARDMEN_SCALE = 8076;

    // Mobs
    private static final int PLAIN_WATCHMAN = 21102;
    private static final int ROCK_GOLEM = 21103;
    private static final int LIZARDMEN_SUPPLIER = 21104;
    private static final int LIZARDMEN_AGENT = 21105;
    private static final int CURSED_SEER = 21106;
    private static final int LIZARDMEN_COMMANDER = 21107;
    private static final int LIZARDMEN_SHAMAN = 20781;

    // Rewards
    private static final int ENCHANT_WEAPON_D = 955;
    private static final int ENCHANT_ARMOR_D = 956;

    public Q660_AidingTheFloranVillage() {
        super(660, "Aiding the Floran Village");

        setItemsIds(WATCHING_EYES, LIZARDMEN_SCALE, GOLEM_SHARD);

        addStartNpc(MARIA, ALEX);
        addTalkId(MARIA, ALEX);

        addKillId(CURSED_SEER, PLAIN_WATCHMAN, ROCK_GOLEM, LIZARDMEN_SHAMAN, LIZARDMEN_SUPPLIER, LIZARDMEN_COMMANDER, LIZARDMEN_AGENT);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30608-04.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
        }
        else if (event.equalsIgnoreCase("30291-02.htm")) {
            if (player.getLevel() < 30) { htmltext = "30291-02a.htm"; }
            else {
                st.setState(QuestState.STATE_STARTED);
                st.set("cond", "2");
                st.playSound(ESound.ItemSound_quest_accept);
            }
        }
        else if (event.equalsIgnoreCase("30291-05.htm")) {
            int count = st.getQuestItemsCount(WATCHING_EYES) + st.getQuestItemsCount(LIZARDMEN_SCALE) + st.getQuestItemsCount(GOLEM_SHARD);
            if (count == 0) { htmltext = "30291-05a.htm"; }
            else {
                st.takeItems(GOLEM_SHARD, -1);
                st.takeItems(LIZARDMEN_SCALE, -1);
                st.takeItems(WATCHING_EYES, -1);
                st.rewardItems(ItemConst.ADENA_ID, count * 100 + ((count >= 45) ? 9000 : 0));
            }
        }
        else if (event.equalsIgnoreCase("30291-06.htm")) {
            st.playSound(ESound.ItemSound_quest_finish);
            st.exitQuest(true);
        }
        else if (event.equalsIgnoreCase("30291-11.htm")) {
            if (!verifyAndRemoveItems(st, 100)) { htmltext = "30291-11a.htm"; }
            else {
                if (Rnd.get(10) < 8) { st.rewardItems(ItemConst.ADENA_ID, 1000); }
                else {
                    st.rewardItems(ItemConst.ADENA_ID, 13000);
                    st.rewardItems(ENCHANT_ARMOR_D, 1);
                }
            }
        }
        else if (event.equalsIgnoreCase("30291-12.htm")) {
            if (!verifyAndRemoveItems(st, 200)) { htmltext = "30291-12a.htm"; }
            else {
                int luck = Rnd.get(15);
                if (luck < 8) { st.rewardItems(ItemConst.ADENA_ID, 2000); }
                else if (luck < 12) {
                    st.rewardItems(ItemConst.ADENA_ID, 20000);
                    st.rewardItems(ENCHANT_ARMOR_D, 1);
                }
                else { st.rewardItems(ENCHANT_WEAPON_D, 1); }
            }
        }
        else if (event.equalsIgnoreCase("30291-13.htm")) {
            if (!verifyAndRemoveItems(st, 500)) { htmltext = "30291-13a.htm"; }
            else {
                if (Rnd.get(10) < 8) { st.rewardItems(ItemConst.ADENA_ID, 5000); }
                else {
                    st.rewardItems(ItemConst.ADENA_ID, 45000);
                    st.rewardItems(ENCHANT_WEAPON_D, 1);
                }
            }
        }
        else if (event.equalsIgnoreCase("30291-17.htm")) {
            int count = st.getQuestItemsCount(WATCHING_EYES) + st.getQuestItemsCount(LIZARDMEN_SCALE) + st.getQuestItemsCount(GOLEM_SHARD);
            if (count != 0) {
                htmltext = "30291-17a.htm";
                st.takeItems(WATCHING_EYES, -1);
                st.takeItems(LIZARDMEN_SCALE, -1);
                st.takeItems(GOLEM_SHARD, -1);
                st.rewardItems(ItemConst.ADENA_ID, count * 100 + ((count >= 45) ? 9000 : 0));
            }
            st.playSound(ESound.ItemSound_quest_finish);
            st.exitQuest(true);
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
                switch (npc.getNpcId()) {
                    case MARIA:
                        htmltext = (player.getLevel() < 30) ? "30608-01.htm" : "30608-02.htm";
                        break;

                    case ALEX:
                        htmltext = "30291-01.htm";
                        break;
                }
                break;

            case QuestState.STATE_STARTED:
                switch (npc.getNpcId()) {
                    case MARIA:
                        htmltext = "30608-06.htm";
                        break;

                    case ALEX:
                        int cond = st.getInt("cond");
                        if (cond == 1) {
                            htmltext = "30291-03.htm";
                            st.set("cond", "2");
                            st.playSound(ESound.ItemSound_quest_middle);
                        }
                        else if (cond == 2) { htmltext = (st.hasAtLeastOneQuestItem(WATCHING_EYES, LIZARDMEN_SCALE, GOLEM_SHARD)) ? "30291-04.htm" : "30291-05a.htm"; }
                        break;
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        L2PcInstance partyMember = getRandomPartyMember(player, npc, "2");
        if (partyMember == null) { return null; }

        QuestState st = partyMember.getQuestState(qn);

        switch (npc.getNpcId()) {
            case PLAIN_WATCHMAN:
            case CURSED_SEER:
                st.dropItems(WATCHING_EYES, 1, 0, 790000);
                break;

            case ROCK_GOLEM:
                st.dropItems(GOLEM_SHARD, 1, 0, 750000);
                break;

            case LIZARDMEN_SHAMAN:
            case LIZARDMEN_SUPPLIER:
            case LIZARDMEN_AGENT:
            case LIZARDMEN_COMMANDER:
                st.dropItems(LIZARDMEN_SCALE, 1, 0, 670000);
                break;
        }

        return null;
    }

    /**
     * This method drops items following current counts.
     *
     * @param st             The QuestState to affect.
     * @param numberToVerify The count of qItems to drop from the different categories.
     * @return false when counter isn't reached, true otherwise.
     */
    private static boolean verifyAndRemoveItems(QuestState st, int numberToVerify) {
        int eyes = st.getQuestItemsCount(WATCHING_EYES);
        int scale = st.getQuestItemsCount(LIZARDMEN_SCALE);
        int shard = st.getQuestItemsCount(GOLEM_SHARD);

        if (eyes + scale + shard < numberToVerify) { return false; }

        if (eyes >= numberToVerify) { st.takeItems(WATCHING_EYES, numberToVerify); }
        else {
            int currentNumber = numberToVerify - eyes;

            st.takeItems(WATCHING_EYES, -1);
            if (scale >= currentNumber) { st.takeItems(LIZARDMEN_SCALE, currentNumber); }
            else {
                currentNumber -= scale;
                st.takeItems(LIZARDMEN_SCALE, -1);
                st.takeItems(GOLEM_SHARD, currentNumber);
            }
        }
        return true;
    }
}