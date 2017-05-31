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

public class Q363_SorrowfulSoundOfFlute extends Quest
{
	private static final String qn = "Q363_SorrowfulSoundOfFlute";
	
	// NPCs
	private static final int NANARIN = 30956;
	private static final int OPIX = 30595;
	private static final int ALDO = 30057;
	private static final int RANSPO = 30594;
	private static final int HOLVAS = 30058;
	private static final int BARBADO = 30959;
	private static final int POITAN = 30458;
	
	// Item
	private static final int NANARIN_FLUTE = 4319;
	private static final int BLACK_BEER = 4320;
	private static final int CLOTHES = 4318;
	
	// Reward
	private static final int THEME_OF_SOLITUDE = 4420;
	
	public Q363_SorrowfulSoundOfFlute()
	{
		super(363, "Sorrowful Sound of Flute");
		
		setItemsIds(NANARIN_FLUTE, BLACK_BEER, CLOTHES);
		
		addStartNpc(NANARIN);
		addTalkId(NANARIN, OPIX, ALDO, RANSPO, HOLVAS, BARBADO, POITAN);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30956-02.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30956-05.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.giveItems(CLOTHES, 1);
		}
		else if (event.equalsIgnoreCase("30956-06.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.giveItems(NANARIN_FLUTE, 1);
		}
		else if (event.equalsIgnoreCase("30956-07.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.giveItems(BLACK_BEER, 1);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case STATE_CREATED:
				htmltext = (player.getLevel() < 15) ? "30956-03.htm" : "30956-01.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case NANARIN:
						if (cond == 1)
							htmltext = "30956-02.htm";
						else if (cond == 2)
							htmltext = "30956-04.htm";
						else if (cond == 3)
							htmltext = "30956-08.htm";
						else if (cond == 4)
						{
							if (st.getInt("success") == 1)
							{
								htmltext = "30956-09.htm";
								st.giveItems(THEME_OF_SOLITUDE, 1);
								st.playSound(QuestState.SOUND_FINISH);
							}
							else
							{
								htmltext = "30956-10.htm";
								st.playSound(QuestState.SOUND_GIVEUP);
							}
							st.exitQuest(true);
						}
						break;
					
					case OPIX:
					case POITAN:
					case ALDO:
					case RANSPO:
					case HOLVAS:
						htmltext = npc.getNpcId() + "-01.htm";
						if (cond == 1)
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						break;
					
					case BARBADO:
						if (cond == 3)
						{
							st.set("cond", "4");
							st.playSound(QuestState.SOUND_MIDDLE);
							
							if (st.hasQuestItems(NANARIN_FLUTE))
							{
								htmltext = "30959-02.htm";
								st.set("success", "1");
							}
							else
								htmltext = "30959-01.htm";
							
							st.takeItems(BLACK_BEER, -1);
							st.takeItems(CLOTHES, -1);
							st.takeItems(NANARIN_FLUTE, -1);
						}
						else if (cond == 4)
							htmltext = "30959-03.htm";
						break;
				}
		}
		
		return htmltext;
	}
}