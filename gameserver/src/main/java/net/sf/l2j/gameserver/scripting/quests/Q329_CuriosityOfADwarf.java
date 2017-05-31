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

public class Q329_CuriosityOfADwarf extends Quest
{
	private static final String qn = "Q329_CuriosityOfADwarf";
	
	// Items
	private static final int GOLEM_HEARTSTONE = 1346;
	private static final int BROKEN_HEARTSTONE = 1365;
	
	public Q329_CuriosityOfADwarf()
	{
		super(329, "Curiosity of a Dwarf");
		
		addStartNpc(30437); // Rolento
		addTalkId(30437);
		
		addKillId(20083, 20085); // Granite golem, Puncher
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30437-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30437-06.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				htmltext = (player.getLevel() < 33) ? "30437-01.htm" : "30437-02.htm";
				break;
			
			case STATE_STARTED:
				final int golem = st.getQuestItemsCount(GOLEM_HEARTSTONE);
				final int broken = st.getQuestItemsCount(BROKEN_HEARTSTONE);
				
				if (golem + broken == 0)
					htmltext = "30437-04.htm";
				else
				{
					htmltext = "30437-05.htm";
					st.takeItems(GOLEM_HEARTSTONE, -1);
					st.takeItems(BROKEN_HEARTSTONE, -1);
					st.rewardItems(57, broken * 50 + golem * 1000 + ((golem + broken > 10) ? 1183 : 0));
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
		
		final int chance = Rnd.get(100);
		if (chance < 2)
			st.dropItemsAlways(GOLEM_HEARTSTONE, 1, 0);
		else if (chance < ((npc.getNpcId() == 20083) ? 44 : 50))
			st.dropItemsAlways(BROKEN_HEARTSTONE, 1, 0);
		
		return null;
	}
}