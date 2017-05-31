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

public class Q291_RevengeOfTheRedbonnet extends Quest
{
	private static final String qn = "Q291_RevengeOfTheRedbonnet";
	
	// Quest items
	private static final int BLACK_WOLF_PELT = 1482;
	
	// Rewards
	private static final int SCROLL_OF_ESCAPE = 736;
	private static final int GRANDMA_PEARL = 1502;
	private static final int GRANDMA_MIRROR = 1503;
	private static final int GRANDMA_NECKLACE = 1504;
	private static final int GRANDMA_HAIRPIN = 1505;
	
	public Q291_RevengeOfTheRedbonnet()
	{
		super(291, "Revenge of the Redbonnet");
		
		setItemsIds(BLACK_WOLF_PELT);
		
		addStartNpc(30553); // Maryse Redbonnet
		addTalkId(30553);
		
		addKillId(20317);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30553-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
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
				htmltext = (player.getLevel() < 4) ? "30553-01.htm" : "30553-02.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "30553-04.htm";
				else if (cond == 2)
				{
					htmltext = "30553-05.htm";
					st.takeItems(BLACK_WOLF_PELT, -1);
					
					int random = Rnd.get(100);
					if (random < 3)
						st.rewardItems(GRANDMA_PEARL, 1);
					else if (random < 21)
						st.rewardItems(GRANDMA_MIRROR, 1);
					else if (random < 46)
						st.rewardItems(GRANDMA_NECKLACE, 1);
					else
					{
						st.rewardItems(SCROLL_OF_ESCAPE, 1);
						st.rewardItems(GRANDMA_HAIRPIN, 1);
					}
					
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = checkPlayerCondition(player, npc, "cond", "1");
		if (st == null)
			return null;
		
		if (st.dropItemsAlways(BLACK_WOLF_PELT, 1, 40))
			st.set("cond", "2");
		
		return null;
	}
}