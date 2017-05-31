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

public class Q601_WatchingEyes extends Quest
{
	private static final String qn = "Q601_WatchingEyes";
	
	// Items
	private static final int PROOF_OF_AVENGER = 7188;
	
	// Rewards
	private static final int[][] REWARDS =
	{
		{
			6699,
			90000,
			20
		},
		{
			6698,
			80000,
			40
		},
		{
			6700,
			40000,
			50
		},
		{
			0,
			230000,
			100
		}
	};
	
	public Q601_WatchingEyes()
	{
		super(601, "Watching Eyes");
		
		setItemsIds(PROOF_OF_AVENGER);
		
		addStartNpc(31683); // Eye of Argos
		addTalkId(31683);
		
		addKillId(21306, 21308, 21309, 21310, 21311);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31683-03.htm"))
		{
			if (player.getLevel() < 71)
				htmltext = "31683-02.htm";
			else
			{
				st.setState(STATE_STARTED);
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
			}
		}
		else if (event.equalsIgnoreCase("31683-07.htm"))
		{
			st.takeItems(PROOF_OF_AVENGER, -1);
			
			final int random = Rnd.get(100);
			for (int[] element : REWARDS)
			{
				if (random < element[2])
				{
					st.rewardItems(57, element[1]);
					
					if (element[0] != 0)
					{
						st.giveItems(element[0], 5);
						st.rewardExpAndSp(120000, 10000);
					}
					break;
				}
			}
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				htmltext = "31683-01.htm";
				break;
			
			case STATE_STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = (st.hasQuestItems(PROOF_OF_AVENGER)) ? "31683-05.htm" : "31683-04.htm";
				else if (cond == 2)
					htmltext = "31683-06.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMember(player, npc, "cond", "1");
		if (partyMember == null)
			return null;
		
		QuestState st = partyMember.getQuestState(qn);
		
		if (st.dropItems(PROOF_OF_AVENGER, 1, 100, 500000))
			st.set("cond", "2");
		
		return null;
	}
}