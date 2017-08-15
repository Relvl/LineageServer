package net.sf.l2j.gameserver.scripting.quests;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q161_FruitOfTheMotherTree extends Quest {
    private static final String qn = "Q161_FruitOfTheMotherTree";

    // NPCs
    private static final int ANDELLIA = 30362;
    private static final int THALIA = 30371;

    // Items
    private static final int ANDELLIA_LETTER = 1036;
    private static final int MOTHERTREE_FRUIT = 1037;

    public Q161_FruitOfTheMotherTree() {
        super(161, "Fruit of the Mothertree");

        setItemsIds(ANDELLIA_LETTER, MOTHERTREE_FRUIT);

        addStartNpc(ANDELLIA);
        addTalkId(ANDELLIA, THALIA);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        String htmltext = event;
        QuestState st = player.getQuestState(qn);
        if (st == null) { return htmltext; }

        if (event.equalsIgnoreCase("30362-04.htm")) {
            st.setState(QuestState.STATE_STARTED);
            st.set("cond", "1");
            st.playSound(QuestState.SOUND_ACCEPT);
            st.giveItems(ANDELLIA_LETTER, 1);
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
                if (player.getRace() != PlayerRace.Elf) { htmltext = "30362-00.htm"; }
                else if (player.getLevel() < 3) { htmltext = "30362-02.htm"; }
                else { htmltext = "30362-03.htm"; }
                break;

            case QuestState.STATE_STARTED:
                int cond = st.getInt("cond");
                switch (npc.getNpcId()) {
                    case ANDELLIA:
                        if (cond == 1) { htmltext = "30362-05.htm"; }
                        else if (cond == 2) {
                            htmltext = "30362-06.htm";
                            st.takeItems(MOTHERTREE_FRUIT, 1);
                            st.rewardItems(ItemConst.ADENA_ID, 1000);
                            st.rewardExpAndSp(1000, 0);
                            st.playSound(QuestState.SOUND_FINISH);
                            st.exitQuest(false);
                        }
                        break;

                    case THALIA:
                        if (cond == 1) {
                            htmltext = "30371-01.htm";
                            st.set("cond", "2");
                            st.playSound(QuestState.SOUND_MIDDLE);
                            st.takeItems(ANDELLIA_LETTER, 1);
                            st.giveItems(MOTHERTREE_FRUIT, 1);
                        }
                        else if (cond == 2) { htmltext = "30371-02.htm"; }
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