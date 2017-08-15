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

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q004_LongliveThePaagrioLord extends Quest
{
	private static final String qn = "Q004_LongliveThePaagrioLord";
	
	private static final Map<Integer, Integer> NPC_GIFTS = new HashMap<>();
	{
		NPC_GIFTS.put(30585, 1542);
		NPC_GIFTS.put(30566, 1541);
		NPC_GIFTS.put(30562, 1543);
		NPC_GIFTS.put(30560, 1544);
		NPC_GIFTS.put(30559, 1545);
		NPC_GIFTS.put(30587, 1546);
	}
	
	public Q004_LongliveThePaagrioLord()
	{
		super(4, "Long live the Pa'agrio Lord!");
		
		setItemsIds(1541, 1542, 1543, 1544, 1545, 1546);
		
		addStartNpc(30578); // Nakusin
		addTalkId(30578, 30585, 30566, 30562, 30560, 30559, 30587);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30578-03.htm"))
		{
			st.setState(QuestState.STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getRace() != PlayerRace.Orc)
					htmltext = "30578-00.htm";
				else if (player.getLevel() < 2)
					htmltext = "30578-01.htm";
				else
					htmltext = "30578-02.htm";
				break;
			
			case QuestState.STATE_STARTED:
				int cond = st.getInt("cond");
				int npcId = npc.getNpcId();
				
				if (npcId == 30578)
				{
					if (cond == 1)
						htmltext = "30578-04.htm";
					else if (cond == 2)
					{
						htmltext = "30578-06.htm";
						st.giveItems(4, 1);
						for (int item : NPC_GIFTS.values())
							st.takeItems(item, -1);
						
						st.playSound(QuestState.SOUND_FINISH);
						st.exitQuest(false);
					}
				}
				else
				{
					int i = NPC_GIFTS.get(npcId);
					if (st.hasQuestItems(i))
						htmltext = npcId + "-02.htm";
					else
					{
						st.giveItems(i, 1);
						htmltext = npcId + "-01.htm";
						
						int count = 0;
						for (int item : NPC_GIFTS.values())
							count += st.getQuestItemsCount(item);
						
						if (count == 6)
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				break;
			
			case QuestState.STATE_COMPLETED:
				htmltext = getAlreadyCompletedMsg();
				break;
		}
		
		return htmltext;
	}
}