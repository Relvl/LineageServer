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

public class Q110_ToThePrimevalIsle extends Quest
{
	private static final String qn = "Q110_ToThePrimevalIsle";
	
	// NPCs
	private static final int ANTON = 31338;
	private static final int MARQUEZ = 32113;
	
	// Item
	private static final int ANCIENT_BOOK = 8777;
	
	public Q110_ToThePrimevalIsle()
	{
		super(110, "To the Primeval Isle");
		
		setItemsIds(ANCIENT_BOOK);
		
		addStartNpc(ANTON);
		addTalkId(ANTON, MARQUEZ);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31338-02.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(ANCIENT_BOOK, 1);
		}
		else if (event.equalsIgnoreCase("32113-03.htm") && st.hasQuestItems(ANCIENT_BOOK))
		{
			st.takeItems(ANCIENT_BOOK, 1);
			st.rewardItems(57, 169380);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				htmltext = (player.getLevel() < 75) ? "31338-00.htm" : "31338-01.htm";
				break;
			
			case STATE_STARTED:
				switch (npc.getNpcId())
				{
					case ANTON:
						htmltext = "31338-01c.htm";
						break;
					
					case MARQUEZ:
						htmltext = "32113-01.htm";
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