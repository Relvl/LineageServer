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

public class Q295_DreamingOfTheSkies extends Quest
{
	private static final String qn = "Q295_DreamingOfTheSkies";
	
	// Item
	private static final int FLOATING_STONE = 1492;
	
	// Reward
	private static final int RING_OF_FIREFLY = 1509;
	
	public Q295_DreamingOfTheSkies()
	{
		super(295, "Dreaming of the Skies");
		
		setItemsIds(FLOATING_STONE);
		
		addStartNpc(30536); // Arin
		addTalkId(30536);
		
		addKillId(20153); // Magical Weaver
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30536-03.htm"))
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
				htmltext = (player.getLevel() < 11) ? "30536-01.htm" : "30536-02.htm";
				break;
			
			case STATE_STARTED:
				if (st.getInt("cond") == 1)
					htmltext = "30536-04.htm";
				else
				{
					st.takeItems(FLOATING_STONE, -1);
					
					if (!st.hasQuestItems(RING_OF_FIREFLY))
					{
						htmltext = "30536-05.htm";
						st.giveItems(RING_OF_FIREFLY, 1);
					}
					else
					{
						htmltext = "30536-06.htm";
						st.rewardItems(57, 2400);
					}
					
					st.rewardExpAndSp(0, 500);
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
		
		if (st.dropItemsAlways(FLOATING_STONE, (Rnd.get(100) > 25) ? 1 : 2, 50))
			st.set("cond", "2");
		
		return null;
	}
}