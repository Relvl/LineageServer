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
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q376_ExplorationOfTheGiantsCave_Part1 extends Quest
{
	private static final String qn = "Q376_ExplorationOfTheGiantsCave_Part1";
	
	// NPCs
	private static final int SOBLING = 31147;
	private static final int CLIFF = 30182;
	
	// Items
	private static final int PARCHMENT = 5944;
	private static final int DICTIONARY_BASIC = 5891;
	private static final int MYSTERIOUS_BOOK = 5890;
	private static final int DICTIONARY_INTERMEDIATE = 5892;
	private static final int[][] BOOKS =
	{
		// medical theory -> tallum tunic, tallum stockings
		{
			5937,
			5938,
			5939,
			5940,
			5941
		},
		// architecture -> dark crystal leather, tallum leather
		{
			5932,
			5933,
			5934,
			5935,
			5936
		},
		// golem plans -> dark crystal breastplate, tallum plate
		{
			5922,
			5923,
			5924,
			5925,
			5926
		},
		// basics of magic -> dark crystal gaiters, dark crystal leggings
		{
			5927,
			5928,
			5929,
			5930,
			5931
		}
	};
	
	// Rewards
	private static final int[][] RECIPES =
	{
		// medical theory -> tallum tunic, tallum stockings
		{
			5346,
			5354
		},
		// architecture -> dark crystal leather, tallum leather
		{
			5332,
			5334
		},
		// golem plans -> dark crystal breastplate, tallum plate
		{
			5416,
			5418
		},
		// basics of magic -> dark crystal gaiters, dark crystal leggings
		{
			5424,
			5340
		}
	};
	
	public Q376_ExplorationOfTheGiantsCave_Part1()
	{
		super(376, "Exploration of the Giants' Cave, Part 1");
		
		setItemsIds(DICTIONARY_BASIC, MYSTERIOUS_BOOK);
		
		addStartNpc(SOBLING);
		addTalkId(SOBLING, CLIFF);
		
		addKillId(20647, 20648, 20649, 20650);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		// Sobling
		if (event.equalsIgnoreCase("31147-03.htm"))
		{
			st.setState(QuestState.STATE_STARTED);
			st.set("cond", "1");
			st.set("condBook", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(DICTIONARY_BASIC, 1);
		}
		else if (event.equalsIgnoreCase("31147-04.htm"))
		{
			htmltext = checkItems(st);
		}
		else if (event.equalsIgnoreCase("31147-09.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		// Cliff
		else if (event.equalsIgnoreCase("30182-02.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(MYSTERIOUS_BOOK, -1);
			st.giveItems(DICTIONARY_INTERMEDIATE, 1);
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
			case QuestState.STATE_CREATED:
				htmltext = (player.getLevel() < 51) ? "31147-01.htm" : "31147-02.htm";
				break;
			
			case QuestState.STATE_STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case SOBLING:
						htmltext = checkItems(st);
						break;
					
					case CLIFF:
						if (cond == 2 && st.hasQuestItems(MYSTERIOUS_BOOK))
							htmltext = "30182-01.htm";
						else if (cond == 3)
							htmltext = "30182-03.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		// Drop parchment to anyone
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STATE_STARTED);
		if (partyMember == null)
			return null;
		
		QuestState st = partyMember.getQuestState(qn);
		
		st.dropItems(PARCHMENT, 1, 0, 20000);
		
		// Drop mysterious book to person who still need it
		partyMember = getRandomPartyMember(player, npc, "condBook", "1");
		if (partyMember == null)
			return null;
		
		st = partyMember.getQuestState(qn);
		
		if (st.dropItems(MYSTERIOUS_BOOK, 1, 1, 1000))
			st.unset("condBook");
		
		return null;
	}
	
	private static String checkItems(QuestState st)
	{
		if (st.hasQuestItems(MYSTERIOUS_BOOK))
		{
			int cond = st.getInt("cond");
			if (cond == 1)
			{
				st.set("cond", "2");
				st.playSound(QuestState.SOUND_MIDDLE);
				return "31147-07.htm";
			}
			return "31147-08.htm";
		}
		
		for (int type = 0; type < BOOKS.length; type++)
		{
			boolean complete = true;
			for (int book : BOOKS[type])
			{
				if (!st.hasQuestItems(book))
					complete = false;
			}
			
			if (complete)
			{
				for (int book : BOOKS[type])
					st.takeItems(book, 1);
				
				st.giveItems(RECIPES[type][Rnd.get(RECIPES[type].length)], 1);
				return "31147-04.htm";
			}
		}
		return "31147-05.htm";
	}
}