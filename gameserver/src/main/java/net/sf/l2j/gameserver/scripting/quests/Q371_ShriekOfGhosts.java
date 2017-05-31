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

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q371_ShriekOfGhosts extends Quest
{
	private static final String qn = "Q371_ShriekOfGhosts";
	
	// NPCs
	private static final int REVA = 30867;
	private static final int PATRIN = 30929;
	
	// Item
	private static final int URN = 5903;
	private static final int PORCELAIN = 6002;
	
	// Drop chances
	private static final Map<Integer, int[]> CHANCES = new HashMap<>();
	{
		CHANCES.put(20818, new int[]
		{
			38,
			43
		});
		CHANCES.put(20820, new int[]
		{
			48,
			56
		});
		CHANCES.put(20824, new int[]
		{
			50,
			58
		});
	}
	
	public Q371_ShriekOfGhosts()
	{
		super(371, "Shriek of Ghosts");
		
		setItemsIds(URN, PORCELAIN);
		
		addStartNpc(REVA);
		addTalkId(REVA, PATRIN);
		
		addKillId(20818, 20820, 20824);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30867-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30867-07.htm"))
		{
			int urns = st.getQuestItemsCount(URN);
			if (urns > 0)
			{
				st.takeItems(URN, urns);
				if (urns >= 100)
				{
					urns += 13;
					htmltext = "30867-08.htm";
				}
				else
					urns += 7;
				st.rewardItems(57, urns * 1000);
			}
		}
		else if (event.equalsIgnoreCase("30867-10.htm"))
		{
			st.playSound(QuestState.SOUND_GIVEUP);
			st.exitQuest(true);
		}
		else if (event.equalsIgnoreCase("APPR"))
		{
			if (st.hasQuestItems(PORCELAIN))
			{
				int chance = Rnd.get(100);
				
				st.takeItems(PORCELAIN, 1);
				
				if (chance < 2)
				{
					st.giveItems(6003, 1);
					htmltext = "30929-03.htm";
				}
				else if (chance < 32)
				{
					st.giveItems(6004, 1);
					htmltext = "30929-04.htm";
				}
				else if (chance < 62)
				{
					st.giveItems(6005, 1);
					htmltext = "30929-05.htm";
				}
				else if (chance < 77)
				{
					st.giveItems(6006, 1);
					htmltext = "30929-06.htm";
				}
				else
					htmltext = "30929-07.htm";
			}
			else
				htmltext = "30929-02.htm";
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
				htmltext = (player.getLevel() < 59) ? "30867-01.htm" : "30867-02.htm";
				break;
			
			case STATE_STARTED:
				switch (npc.getNpcId())
				{
					case REVA:
						if (st.hasQuestItems(URN))
							htmltext = (st.hasQuestItems(PORCELAIN)) ? "30867-05.htm" : "30867-04.htm";
						else
							htmltext = "30867-06.htm";
						break;
					
					case PATRIN:
						htmltext = "30929-01.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, STATE_STARTED);
		if (partyMember == null)
			return null;
		
		QuestState st = partyMember.getQuestState(qn);
		
		final int[] chances = CHANCES.get(npc.getNpcId());
		final int random = Rnd.get(100);
		
		if (random < chances[1])
			st.dropItemsAlways((random < chances[0]) ? URN : PORCELAIN, 1, 0);
		
		return null;
	}
}