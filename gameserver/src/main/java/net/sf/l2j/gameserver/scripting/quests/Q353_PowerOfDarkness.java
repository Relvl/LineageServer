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

public class Q353_PowerOfDarkness extends Quest
{
	private static final String qn = "Q353_PowerOfDarkness";
	
	// Item
	private static final int STONE = 5862;
	
	public Q353_PowerOfDarkness()
	{
		super(353, "Power of Darkness");
		
		setItemsIds(STONE);
		
		addStartNpc(31044); // Galman
		addTalkId(31044);
		
		addKillId(20244, 20245, 20283, 20284);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31044-04.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31044-08.htm"))
		{
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
				htmltext = (player.getLevel() < 55) ? "31044-01.htm" : "31044-02.htm";
				break;
			
			case STATE_STARTED:
				final int stones = st.getQuestItemsCount(STONE);
				if (stones == 0)
					htmltext = "31044-05.htm";
				else
				{
					htmltext = "31044-06.htm";
					st.takeItems(STONE, -1);
					st.rewardItems(57, 2500 + 230 * stones);
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = checkPlayerState(player, npc, STATE_STARTED);
		if (st == null)
			return null;
		
		st.dropItems(STONE, 1, 0, (npc.getNpcId() == 20244 || npc.getNpcId() == 20283) ? 480000 : 500000);
		
		return null;
	}
}