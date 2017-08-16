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

public class Q225_TestOfTheSearcher extends Quest {
    private static final String qn = "Q225_TestOfTheSearcher";

    // Items
    private static final int LUTHER_LETTER = 2784;
    private static final int ALEX_WARRANT = 2785;
    private static final int LEIRYNN_ORDER_1 = 2786;
    private static final int DELU_TOTEM = 2787;
    private static final int LEIRYNN_ORDER_2 = 2788;
    private static final int CHIEF_KALKI_FANG = 2789;
    private static final int LEIRYNN_REPORT = 2790;
    private static final int STRANGE_MAP = 2791;
    private static final int LAMBERT_MAP = 2792;
    private static final int ALEX_LETTER = 2793;
    private static final int ALEX_ORDER = 2794;
    private static final int WINE_CATALOG = 2795;
    private static final int TYRA_CONTRACT = 2796;
    private static final int RED_SPORE_DUST = 2797;
    private static final int MALRUKIAN_WINE = 2798;
    private static final int OLD_ORDER = 2799;
    private static final int JAX_DIARY = 2800;
    private static final int TORN_MAP_PIECE_1 = 2801;
    private static final int TORN_MAP_PIECE_2 = 2802;
    private static final int SOLT_MAP = 2803;
    private static final int MAKEL_MAP = 2804;
    private static final int COMBINED_MAP = 2805;
    private static final int RUSTED_KEY = 2806;
    private static final int GOLD_BAR = 2807;
    private static final int ALEX_RECOMMEND = 2808;

    // Rewards
    private static final int MARK_OF_SEARCHER = 2809;
    private static final int DIMENSIONAL_DIAMOND = 7562;

    // NPCs
    private static final int ALEX = 30291;
    private static final int TYRA = 30420;
    private static final int TREE = 30627;
    private static final int STRONG_WOODEN_CHEST = 30628;
    private static final int LUTHER = 30690;
    private static final int LEIRYNN = 30728;
    private static final int BORYS = 30729;
    private static final int JAX = 30730;

    // Monsters
    private static final int HANGMAN_TREE = 20144;
    private static final int ROAD_SCAVENGER = 20551;
    private static final int GIANT_FUNGUS = 20555;
    private static final int DELU_LIZARDMAN_SHAMAN = 20781;
    private static final int DELU_CHIEF_KALKIS = 27093;
    private static final int NEER_BODYGUARD = 27092;

    private static L2Npc _strongWoodenChest; // Used to avoid to spawn multiple instances.

    public Q225_TestOfTheSearcher() {
        super(225, "Test of the Searcher");

        setItemsIds(LUTHER_LETTER, ALEX_WARRANT, LEIRYNN_ORDER_1, DELU_TOTEM, LEIRYNN_ORDER_2, CHIEF_KALKI_FANG, LEIRYNN_REPORT, STRANGE_MAP, LAMBERT_MAP, ALEX_LETTER, ALEX_ORDER, WINE_CATALOG, TYRA_CONTRACT, RED_SPORE_DUST, MALRUKIAN_WINE, OLD_ORDER, JAX_DIARY, TORN_MAP_PIECE_1, TORN_MAP_PIECE_2, SOLT_MAP, MAKEL_MAP, COMBINED_MAP, RUSTED_KEY, GOLD_BAR, ALEX_RECOMMEND);

        addStartNpc(LUTHER);
        addTalkId(ALEX, TYRA, TREE, STRONG_WOODEN_CHEST, LUTHER, LEIRYNN, BORYS, JAX);

        addAttackId(DELU_LIZARDMAN_SHAMAN);
        addKillId(HANGMAN_TREE, ROAD_SCAVENGER, GIANT_FUNGUS, DELU_LIZARDMAN_SHAMAN, DELU_CHIEF_KALKIS, NEER_BODYGUARD);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        // LUTHER
        if (event.equalsIgnoreCase("30690-05a.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(ESound.ItemSound_quest_accept);
            st.giveItems(LUTHER_LETTER, 1);
            st.giveItems(DIMENSIONAL_DIAMOND, (player.getClassId() == ClassId.scavenger) ? 82 : 96);
        }
        // ALEX
        else if (event.equalsIgnoreCase("30291-07.htm")) {
            st.set("cond", "8");
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(LEIRYNN_REPORT, 1);
            st.takeItems(STRANGE_MAP, 1);
            st.giveItems(ALEX_LETTER, 1);
            st.giveItems(ALEX_ORDER, 1);
            st.giveItems(LAMBERT_MAP, 1);
        }
        // TYRA
        else if (event.equalsIgnoreCase("30420-01a.htm")) {
            st.set("cond", "10");
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(WINE_CATALOG, 1);
            st.giveItems(TYRA_CONTRACT, 1);
        }
        // JAX
        else if (event.equalsIgnoreCase("30730-01d.htm")) {
            st.set("cond", "14");
            st.playSound(ESound.ItemSound_quest_middle);
            st.takeItems(OLD_ORDER, 1);
            st.giveItems(JAX_DIARY, 1);
        }
        // TREE
        else if (event.equalsIgnoreCase("30627-01a.htm")) {
            if (_strongWoodenChest == null) {
                if (st.getInt("cond") == 16) {
                    st.set("cond", "17");
                    st.playSound(ESound.ItemSound_quest_middle);
                    st.giveItems(RUSTED_KEY, 1);
                }

                _strongWoodenChest = addSpawn(STRONG_WOODEN_CHEST, 10098, 157287, -2406, 0, false, 0, true);
                startQuestTimer("chest_despawn", 300000, null, player, false);
            }
        }
        // STRONG WOODEN CHEST
        else if (event.equalsIgnoreCase("30628-01a.htm")) {
            if (!st.hasQuestItems(RUSTED_KEY)) { htmltext = "30628-02.htm"; }
            else {
                st.set("cond", "18");
                st.playSound(ESound.ItemSound_quest_middle);
                st.takeItems(RUSTED_KEY, -1);
                st.giveItems(GOLD_BAR, 20);

                _strongWoodenChest.deleteMe();
                _strongWoodenChest = null;
                cancelQuestTimer("chest_despawn", null, player);
            }
        }
        // STRONG WOODEN CHEST DESPAWN
        else if (event.equalsIgnoreCase("chest_despawn")) {
            _strongWoodenChest.deleteMe();
            _strongWoodenChest = null;
            return null;
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
                if (player.getClassId() != ClassId.rogue && player.getClassId() != ClassId.elvenScout && player.getClassId() != ClassId.assassin && player.getClassId() != ClassId.scavenger) { htmltext = "30690-01.htm"; }
                else if (player.getLevel() < 39) { htmltext = "30690-02.htm"; }
                else { htmltext = (player.getClassId() == ClassId.scavenger) ? "30690-04.htm" : "30690-03.htm"; }
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case LUTHER:
                        if (cond == 1) { htmltext = "30690-06.htm"; }
                        else if (cond > 1 && cond < 19) { htmltext = "30690-07.htm"; }
                        else if (cond == 19) {
                            htmltext = "30690-08.htm";
                            st.takeItems(ALEX_RECOMMEND, 1);
                            st.giveItems(MARK_OF_SEARCHER, 1);
                            st.rewardExpAndSp(37831, 18750);
                            player.broadcastPacket(new SocialAction(player, 3));
                            st.playSound(ESound.ItemSound_quest_finish);
                            st.exitQuest(false);
                        }
                        break;

                    case ALEX:
                        if (cond == 1) {
                            htmltext = "30291-01.htm";
                            st.set("cond", "2");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(LUTHER_LETTER, 1);
                            st.giveItems(ALEX_WARRANT, 1);
                        }
                        else if (cond == 2) { htmltext = "30291-02.htm"; }
                        else if (cond > 2 && cond < 7) { htmltext = "30291-03.htm"; }
                        else if (cond == 7) { htmltext = "30291-04.htm"; }
                        else if (cond > 7 && cond < 13) { htmltext = "30291-08.htm"; }
                        else if (cond > 12 && cond < 16) { htmltext = "30291-09.htm"; }
                        else if (cond > 15 && cond < 18) { htmltext = "30291-10.htm"; }
                        else if (cond == 18) {
                            htmltext = "30291-11.htm";
                            st.set("cond", "19");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(ALEX_ORDER, 1);
                            st.takeItems(COMBINED_MAP, 1);
                            st.takeItems(GOLD_BAR, -1);
                            st.giveItems(ALEX_RECOMMEND, 1);
                        }
                        else if (cond == 19) { htmltext = "30291-12.htm"; }
                        break;

                    case LEIRYNN:
                        if (cond == 2) {
                            htmltext = "30728-01.htm";
                            st.set("cond", "3");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(ALEX_WARRANT, 1);
                            st.giveItems(LEIRYNN_ORDER_1, 1);
                        }
                        else if (cond == 3) { htmltext = "30728-02.htm"; }
                        else if (cond == 4) {
                            htmltext = "30728-03.htm";
                            st.set("cond", "5");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(DELU_TOTEM, -1);
                            st.takeItems(LEIRYNN_ORDER_1, 1);
                            st.giveItems(LEIRYNN_ORDER_2, 1);
                        }
                        else if (cond == 5) { htmltext = "30728-04.htm"; }
                        else if (cond == 6) {
                            htmltext = "30728-05.htm";
                            st.set("cond", "7");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(CHIEF_KALKI_FANG, 1);
                            st.takeItems(LEIRYNN_ORDER_2, 1);
                            st.giveItems(LEIRYNN_REPORT, 1);
                        }
                        else if (cond == 7) { htmltext = "30728-06.htm"; }
                        else if (cond > 7) { htmltext = "30728-07.htm"; }
                        break;

                    case BORYS:
                        if (cond == 8) {
                            htmltext = "30729-01.htm";
                            st.set("cond", "9");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(ALEX_LETTER, 1);
                            st.giveItems(WINE_CATALOG, 1);
                        }
                        else if (cond > 8 && cond < 12) { htmltext = "30729-02.htm"; }
                        else if (cond == 12) {
                            htmltext = "30729-03.htm";
                            st.set("cond", "13");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(MALRUKIAN_WINE, 1);
                            st.takeItems(WINE_CATALOG, 1);
                            st.giveItems(OLD_ORDER, 1);
                        }
                        else if (cond == 13) { htmltext = "30729-04.htm"; }
                        else if (cond > 13) { htmltext = "30729-05.htm"; }
                        break;

                    case TYRA:
                        if (cond == 9) { htmltext = "30420-01.htm"; }
                        else if (cond == 10) { htmltext = "30420-02.htm"; }
                        else if (cond == 11) {
                            htmltext = "30420-03.htm";
                            st.set("cond", "12");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(RED_SPORE_DUST, -1);
                            st.takeItems(TYRA_CONTRACT, 1);
                            st.giveItems(MALRUKIAN_WINE, 1);
                        }
                        else if (cond > 11) { htmltext = "30420-04.htm"; }
                        break;

                    case JAX:
                        if (cond == 13) { htmltext = "30730-01.htm"; }
                        else if (cond == 14) { htmltext = "30730-02.htm"; }
                        else if (cond == 15) {
                            htmltext = "30730-03.htm";
                            st.set("cond", "16");
                            st.playSound(ESound.ItemSound_quest_middle);
                            st.takeItems(LAMBERT_MAP, 1);
                            st.takeItems(MAKEL_MAP, 1);
                            st.takeItems(JAX_DIARY, 1);
                            st.takeItems(SOLT_MAP, 1);
                            st.giveItems(COMBINED_MAP, 1);
                        }
                        else if (cond > 15) { htmltext = "30730-04.htm"; }
                        break;

                    case TREE:
                        if (cond == 16 || cond == 17) { htmltext = "30627-01.htm"; }
                        break;

                    case STRONG_WOODEN_CHEST:
                        if (cond == 17) { htmltext = "30628-01.htm"; }
                        break;
                }
                break;

            case QuestState.STATE_COMPLETED:
                htmltext = getAlreadyCompletedMsg();
                break;
        }

        return htmltext;
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) {
        QuestState st = checkPlayerState(attacker, npc, QuestState.STATE_STARTED);
        if (st == null) { return null; }

        if (st.hasQuestItems(LEIRYNN_ORDER_1) && !npc.isScriptValue(1)) {
            npc.setScriptValue(1);
            addSpawn(NEER_BODYGUARD, npc, false, 200000, true);
        }

        return null;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
        QuestState st;

        switch (npc.getNpcId()) {
            case DELU_LIZARDMAN_SHAMAN:
                st = checkPlayerCondition(player, npc, "cond", "3");
                if (st == null) { return null; }

                if (st.dropItemsAlways(DELU_TOTEM, 1, 10)) { st.set("cond", "4"); }
                break;

            case DELU_CHIEF_KALKIS:
                st = checkPlayerCondition(player, npc, "cond", "5");
                if (st == null) { return null; }

                st.set("cond", "6");
                st.playSound(ESound.ItemSound_quest_middle);
                st.giveItems(CHIEF_KALKI_FANG, 1);
                st.giveItems(STRANGE_MAP, 1);
                break;

            case GIANT_FUNGUS:
                st = checkPlayerCondition(player, npc, "cond", "10");
                if (st == null) { return null; }

                if (st.dropItemsAlways(RED_SPORE_DUST, 1, 10)) { st.set("cond", "11"); }
                break;

            case ROAD_SCAVENGER:
                st = checkPlayerCondition(player, npc, "cond", "14");
                if (st == null) { return null; }

                if (!st.hasQuestItems(SOLT_MAP) && st.dropItems(TORN_MAP_PIECE_1, 1, 4, 500000)) {
                    st.takeItems(TORN_MAP_PIECE_1, -1);
                    st.giveItems(SOLT_MAP, 1);

                    if (st.hasQuestItems(MAKEL_MAP)) { st.set("cond", "15"); }
                }
                break;

            case HANGMAN_TREE:
                st = checkPlayerCondition(player, npc, "cond", "14");
                if (st == null) { return null; }

                if (!st.hasQuestItems(MAKEL_MAP) && st.dropItems(TORN_MAP_PIECE_2, 1, 4, 500000)) {
                    st.takeItems(TORN_MAP_PIECE_2, -1);
                    st.giveItems(MAKEL_MAP, 1);

                    if (st.hasQuestItems(SOLT_MAP)) { st.set("cond", "15"); }
                }
                break;
        }

        return null;
    }
}