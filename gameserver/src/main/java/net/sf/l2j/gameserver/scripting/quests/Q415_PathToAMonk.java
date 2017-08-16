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
import net.sf.l2j.gameserver.model.item.type.EWeaponType;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound.ESound;
import net.sf.l2j.gameserver.network.client.game_to_client.SocialAction;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q415_PathToAMonk extends Quest {
    private static final String qn = "Q415_PathToAMonk";

    // Items
    private static final int POMEGRANATE = 1593;
    private static final int LEATHER_POUCH_1 = 1594;
    private static final int LEATHER_POUCH_2 = 1595;
    private static final int LEATHER_POUCH_3 = 1596;
    private static final int LEATHER_POUCH_FULL_1 = 1597;
    private static final int LEATHER_POUCH_FULL_2 = 1598;
    private static final int LEATHER_POUCH_FULL_3 = 1599;
    private static final int KASHA_BEAR_CLAW = 1600;
    private static final int KASHA_BLADE_SPIDER_TALON = 1601;
    private static final int SCARLET_SALAMANDER_SCALE = 1602;
    private static final int FIERY_SPIRIT_SCROLL = 1603;
    private static final int ROSHEEK_LETTER = 1604;
    private static final int GANTAKI_LETTER_OF_RECOMMENDATION = 1605;
    private static final int FIG = 1606;
    private static final int LEATHER_POUCH_4 = 1607;
    private static final int LEATHER_POUCH_FULL_4 = 1608;
    private static final int VUKU_ORC_TUSK = 1609;
    private static final int RATMAN_FANG = 1610;
    private static final int LANG_KLIZARDMAN_TOOTH = 1611;
    private static final int FELIM_LIZARDMAN_TOOTH = 1612;
    private static final int IRON_WILL_SCROLL = 1613;
    private static final int TORUKU_LETTER = 1614;
    private static final int KHAVATARI_TOTEM = 1615;
    private static final int KASHA_SPIDER_TOOTH = 8545;
    private static final int HORN_OF_BAAR_DRE_VANUL = 8546;

    // NPCs
    private static final int GANTAKI = 30587;
    private static final int ROSHEEK = 30590;
    private static final int KASMAN = 30501;
    private static final int TORUKU = 30591;
    private static final int AREN = 32056;
    private static final int MOIRA = 31979;

    public Q415_PathToAMonk() {
        super(415, "Path to a Monk");

        setItemsIds(POMEGRANATE, LEATHER_POUCH_1, LEATHER_POUCH_2, LEATHER_POUCH_3, LEATHER_POUCH_FULL_1, LEATHER_POUCH_FULL_2, LEATHER_POUCH_FULL_3, KASHA_BEAR_CLAW, KASHA_BLADE_SPIDER_TALON, SCARLET_SALAMANDER_SCALE, FIERY_SPIRIT_SCROLL, ROSHEEK_LETTER, GANTAKI_LETTER_OF_RECOMMENDATION, FIG, LEATHER_POUCH_4, LEATHER_POUCH_FULL_4, VUKU_ORC_TUSK, RATMAN_FANG, LANG_KLIZARDMAN_TOOTH, FELIM_LIZARDMAN_TOOTH, IRON_WILL_SCROLL, TORUKU_LETTER, KASHA_SPIDER_TOOTH, HORN_OF_BAAR_DRE_VANUL);

        addStartNpc(GANTAKI);
        addTalkId(GANTAKI, ROSHEEK, KASMAN, TORUKU, AREN, MOIRA);

        addKillId(20014, 20017, 20024, 20359, 20415, 20476, 20478, 20479, 21118);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30587-05.htm")) {
            if (player.getClassId() != ClassId.orcFighter) { htmltext = (player.getClassId() == ClassId.orcMonk) ? "30587-02a.htm" : "30587-02.htm"; }
            else if (player.getLevel() < 19) { htmltext = "30587-03.htm"; }
            else if (st.hasQuestItems(KHAVATARI_TOTEM)) { htmltext = "30587-04.htm"; }
        }
        else if (event.equalsIgnoreCase("30587-06.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
            st.giveItems(POMEGRANATE, 1);
        }
        else if (event.equalsIgnoreCase("30587-09a.htm")) {
            st.set("cond", "9");
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(ROSHEEK_LETTER, 1);
            st.giveItems(GANTAKI_LETTER_OF_RECOMMENDATION, 1);
        }
        else if (event.equalsIgnoreCase("30587-09b.htm")) {
            st.set("cond", "14");
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(ROSHEEK_LETTER, 1);
        }
        else if (event.equalsIgnoreCase("32056-03.htm")) {
            st.set("cond", "15");
            st.playSound(ESound.ItemSound_quest_middle);
        }
        else if (event.equalsIgnoreCase("32056-08.htm")) {
            st.set("cond", "20");
            st.playSound(ESound.ItemSound_quest_middle);
        }
        else if (event.equalsIgnoreCase("31979-03.htm")) {
            st.takeItems(FIERY_SPIRIT_SCROLL, 1);
            st.giveItems(KHAVATARI_TOTEM, 1);
            st.rewardExpAndSp(3200, 4230);
            player.broadcastPacket(new SocialAction(player, 3));
            st.playSound(ESound.ItemSound_quest_finish);
            st.exitQuest(true);
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
                htmltext = "30587-01.htm";
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case GANTAKI:
                        if (cond == 1) { htmltext = "30587-07.htm"; }
                        else if (cond > 1 && cond < 8) { htmltext = "30587-08.htm"; }
                        else if (cond == 8) { htmltext = "30587-09.htm"; }
                        else if (cond == 9) { htmltext = "30587-10.htm"; }
                        else if (cond > 9) { htmltext = "30587-11.htm"; }
                        break;

                    case ROSHEEK:
                        if (cond == 1) {
                            htmltext = "30590-01.htm";
                            st.set("cond", "2");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(POMEGRANATE, 1);
                            st.giveItems(LEATHER_POUCH_1, 1);
                        }
                        else if (cond == 2) { htmltext = "30590-02.htm"; }
                        else if (cond == 3) {
                            htmltext = "30590-03.htm";
                            st.set("cond", "4");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(LEATHER_POUCH_FULL_1, 1);
                            st.giveItems(LEATHER_POUCH_2, 1);
                        }
                        else if (cond == 4) { htmltext = "30590-04.htm"; }
                        else if (cond == 5) {
                            htmltext = "30590-05.htm";
                            st.set("cond", "6");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(LEATHER_POUCH_FULL_2, 1);
                            st.giveItems(LEATHER_POUCH_3, 1);
                        }
                        else if (cond == 6) { htmltext = "30590-06.htm"; }
                        else if (cond == 7) {
                            htmltext = "30590-07.htm";
                            st.set("cond", "8");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(LEATHER_POUCH_FULL_3, 1);
                            st.giveItems(FIERY_SPIRIT_SCROLL, 1);
                            st.giveItems(ROSHEEK_LETTER, 1);
                        }
                        else if (cond == 8) { htmltext = "30590-08.htm"; }
                        else if (cond > 8) { htmltext = "30590-09.htm"; }
                        break;

                    case KASMAN:
                        if (cond == 9) {
                            htmltext = "30501-01.htm";
                            st.set("cond", "10");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(GANTAKI_LETTER_OF_RECOMMENDATION, 1);
                            st.giveItems(FIG, 1);
                        }
                        else if (cond == 10) { htmltext = "30501-02.htm"; }
                        else if (cond == 11 || cond == 12) { htmltext = "30501-03.htm"; }
                        else if (cond == 13) {
                            htmltext = "30501-04.htm";
                            st.takeItems(FIERY_SPIRIT_SCROLL, 1);
                            st.takeItems(IRON_WILL_SCROLL, 1);
                            st.takeItems(TORUKU_LETTER, 1);
                            st.giveItems(KHAVATARI_TOTEM, 1);
                            st.rewardExpAndSp(3200, 1500);
                            player.broadcastPacket(new SocialAction(player, 3));
                            st.playSound(ESound.ItemSound_quest_finish);
                            st.exitQuest(true);
                        }
                        break;

                    case TORUKU:
                        if (cond == 10) {
                            htmltext = "30591-01.htm";
                            st.set("cond", "11");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(FIG, 1);
                            st.giveItems(LEATHER_POUCH_4, 1);
                        }
                        else if (cond == 11) { htmltext = "30591-02.htm"; }
                        else if (cond == 12) {
                            htmltext = "30591-03.htm";
                            st.set("cond", "13");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(LEATHER_POUCH_FULL_4, 1);
                            st.giveItems(IRON_WILL_SCROLL, 1);
                            st.giveItems(TORUKU_LETTER, 1);
                        }
                        else if (cond == 13) { htmltext = "30591-04.htm"; }
                        break;

                    case AREN:
                        if (cond == 14) { htmltext = "32056-01.htm"; }
                        else if (cond == 15) { htmltext = "32056-04.htm"; }
                        else if (cond == 16) {
                            htmltext = "32056-05.htm";
                            st.set("cond", "17");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(KASHA_SPIDER_TOOTH, -1);
                        }
                        else if (cond == 17) { htmltext = "32056-06.htm"; }
                        else if (cond == 18) {
                            htmltext = "32056-07.htm";
                            st.set("cond", "19");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(HORN_OF_BAAR_DRE_VANUL, -1);
                        }
                        else if (cond == 20) { htmltext = "32056-09.htm"; }
                        break;

                    case MOIRA:
                        if (cond == 20) { htmltext = "31979-01.htm"; }
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

        EWeaponType weapon = player.getActiveWeaponItem().getItemType();
        if (!weapon.equals(EWeaponType.DUALFIST) && !weapon.equals(EWeaponType.FIST)) {
            st.playSound(ESound.ItemSound_quest_giveup);
            st.exitQuest(true);
            return null;
        }

        switch (npc.getNpcId()) {
            case 20479:
                if (st.getInt("cond") == 2 && st.dropItemsAlways(KASHA_BEAR_CLAW, 1, 5)) {
                    st.set("cond", "3");
                    st.takeItems(KASHA_BEAR_CLAW, -1);
                    st.takeItems(LEATHER_POUCH_1, 1);
                    st.giveItems(LEATHER_POUCH_FULL_1, 1);
                }
                break;

            case 20478:
                if (st.getInt("cond") == 4 && st.dropItemsAlways(KASHA_BLADE_SPIDER_TALON, 1, 5)) {
                    st.set("cond", "5");
                    st.takeItems(KASHA_BLADE_SPIDER_TALON, -1);
                    st.takeItems(LEATHER_POUCH_2, 1);
                    st.giveItems(LEATHER_POUCH_FULL_2, 1);
                }
                else if (st.getInt("cond") == 15 && st.dropItems(KASHA_SPIDER_TOOTH, 1, 6, 500000)) { st.set("cond", "16"); }
                break;

            case 20476:
                if (st.getInt("cond") == 15 && st.dropItems(KASHA_SPIDER_TOOTH, 1, 6, 500000)) { st.set("cond", "16"); }
                break;

            case 20415:
                if (st.getInt("cond") == 6 && st.dropItemsAlways(SCARLET_SALAMANDER_SCALE, 1, 5)) {
                    st.set("cond", "7");
                    st.takeItems(SCARLET_SALAMANDER_SCALE, -1);
                    st.takeItems(LEATHER_POUCH_3, 1);
                    st.giveItems(LEATHER_POUCH_FULL_3, 1);
                }
                break;

            case 20014:
                if (st.getInt("cond") == 11 && st.dropItemsAlways(FELIM_LIZARDMAN_TOOTH, 1, 3)) {
                    if (st.getQuestItemsCount(RATMAN_FANG) == 3 && st.getQuestItemsCount(LANG_KLIZARDMAN_TOOTH) == 3 && st.getQuestItemsCount(VUKU_ORC_TUSK) == 3) {
                        st.set("cond", "12");
                        st.takeItems(VUKU_ORC_TUSK, -1);
                        st.takeItems(RATMAN_FANG, -1);
                        st.takeItems(LANG_KLIZARDMAN_TOOTH, -1);
                        st.takeItems(FELIM_LIZARDMAN_TOOTH, -1);
                        st.takeItems(LEATHER_POUCH_4, 1);
                        st.giveItems(LEATHER_POUCH_FULL_4, 1);
                    }
                }
                break;

            case 20017:
                if (st.getInt("cond") == 11 && st.dropItemsAlways(VUKU_ORC_TUSK, 1, 3)) {
                    if (st.getQuestItemsCount(RATMAN_FANG) == 3 && st.getQuestItemsCount(LANG_KLIZARDMAN_TOOTH) == 3 && st.getQuestItemsCount(FELIM_LIZARDMAN_TOOTH) == 3) {
                        st.set("cond", "12");
                        st.takeItems(VUKU_ORC_TUSK, -1);
                        st.takeItems(RATMAN_FANG, -1);
                        st.takeItems(LANG_KLIZARDMAN_TOOTH, -1);
                        st.takeItems(FELIM_LIZARDMAN_TOOTH, -1);
                        st.takeItems(LEATHER_POUCH_4, 1);
                        st.giveItems(LEATHER_POUCH_FULL_4, 1);
                    }
                }
                break;

            case 20024:
                if (st.getInt("cond") == 11 && st.dropItemsAlways(LANG_KLIZARDMAN_TOOTH, 1, 3)) {
                    if (st.getQuestItemsCount(RATMAN_FANG) == 3 && st.getQuestItemsCount(FELIM_LIZARDMAN_TOOTH) == 3 && st.getQuestItemsCount(VUKU_ORC_TUSK) == 3) {
                        st.set("cond", "12");
                        st.takeItems(VUKU_ORC_TUSK, -1);
                        st.takeItems(RATMAN_FANG, -1);
                        st.takeItems(LANG_KLIZARDMAN_TOOTH, -1);
                        st.takeItems(FELIM_LIZARDMAN_TOOTH, -1);
                        st.takeItems(LEATHER_POUCH_4, 1);
                        st.giveItems(LEATHER_POUCH_FULL_4, 1);
                    }
                }
                break;

            case 20359:
                if (st.getInt("cond") == 11 && st.dropItemsAlways(RATMAN_FANG, 1, 3)) {
                    if (st.getQuestItemsCount(LANG_KLIZARDMAN_TOOTH) == 3 && st.getQuestItemsCount(FELIM_LIZARDMAN_TOOTH) == 3 && st.getQuestItemsCount(VUKU_ORC_TUSK) == 3) {
                        st.set("cond", "12");
                        st.takeItems(VUKU_ORC_TUSK, -1);
                        st.takeItems(RATMAN_FANG, -1);
                        st.takeItems(LANG_KLIZARDMAN_TOOTH, -1);
                        st.takeItems(FELIM_LIZARDMAN_TOOTH, -1);
                        st.takeItems(LEATHER_POUCH_4, 1);
                        st.giveItems(LEATHER_POUCH_FULL_4, 1);
                    }
                }
                break;

            case 21118:
                if (st.getInt("cond") == 17) {
                    st.set("cond", "18");
                    st.playSound(ESound.ItemSound_quest_middle);
                    st.giveItems(HORN_OF_BAAR_DRE_VANUL, 1);
                }
                break;
        }

        return null;
    }
}