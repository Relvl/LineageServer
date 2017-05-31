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
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q028_ChestCaughtWithABaitOfIcyAir extends Quest
{
	private static final String qn = "Q028_ChestCaughtWithABaitOfIcyAir";
	
	// NPCs
	private static final int OFULLE = 31572;
	private static final int KIKI = 31442;
	
	// Items
	private static final int BIG_YELLOW_TREASURE_CHEST = 6503;
	private static final int KIKI_LETTER = 7626;
	private static final int ELVEN_RING = 881;
	
	public Q028_ChestCaughtWithABaitOfIcyAir()
	{
		super(28, "Chest caught with a bait of icy air");
		
		setItemsIds(KIKI_LETTER);
		
		addStartNpc(OFULLE);
		addTalkId(OFULLE, KIKI);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31572-04.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31572-07.htm"))
		{
			if (st.hasQuestItems(BIG_YELLOW_TREASURE_CHEST))
			{
				st.set("cond", "2");
				st.takeItems(BIG_YELLOW_TREASURE_CHEST, 1);
				st.giveItems(KIKI_LETTER, 1);
			}
			else
				htmltext = "31572-08.htm";
		}
		else if (event.equalsIgnoreCase("31442-02.htm"))
		{
			if (st.hasQuestItems(KIKI_LETTER))
			{
				htmltext = "31442-02.htm";
				st.takeItems(KIKI_LETTER, 1);
				st.giveItems(ELVEN_RING, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "31442-03.htm";
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case STATE_CREATED:
				if (player.getLevel() < 36)
					htmltext = "31572-02.htm";
				else
				{
					QuestState st2 = player.getQuestState("Q051_OFullesSpecialBait");
					if (st2 != null && st2.isCompleted())
						htmltext = "31572-01.htm";
					else
						htmltext = "31572-03.htm";
				}
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case OFULLE:
						if (cond == 1)
							htmltext = (!st.hasQuestItems(BIG_YELLOW_TREASURE_CHEST)) ? "31572-06.htm" : "31572-05.htm";
						else if (cond == 2)
							htmltext = "31572-09.htm";
						break;
					
					case KIKI:
						if (cond == 2)
							htmltext = "31442-01.htm";
						break;
				}
				break;
			
			case STATE_COMPLETED:
				htmltext = getAlreadyCompletedMsg();
				break;
		}
		
		return htmltext;
	}
}