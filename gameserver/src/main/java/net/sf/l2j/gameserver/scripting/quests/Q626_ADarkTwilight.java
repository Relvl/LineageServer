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
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q626_ADarkTwilight extends Quest
{
	private static final String qn = "Q626_ADarkTwilight";
	
	// Items
	private static final int BLOOD_OF_SAINT = 7169;
	
	// NPC
	private static final int HIERARCH = 31517;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	{
		CHANCES.put(21520, 533000);
		CHANCES.put(21523, 566000);
		CHANCES.put(21524, 603000);
		CHANCES.put(21525, 603000);
		CHANCES.put(21526, 587000);
		CHANCES.put(21529, 606000);
		CHANCES.put(21530, 560000);
		CHANCES.put(21531, 669000);
		CHANCES.put(21532, 651000);
		CHANCES.put(21535, 672000);
		CHANCES.put(21536, 597000);
		CHANCES.put(21539, 739000);
		CHANCES.put(21540, 739000);
		CHANCES.put(21658, 669000);
	}
	
	public Q626_ADarkTwilight()
	{
		super(626, "A Dark Twilight");
		
		setItemsIds(BLOOD_OF_SAINT);
		
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH);
		
		for (int npcId : CHANCES.keySet())
			addKillId(npcId);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31517-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("reward1"))
		{
			if (st.getQuestItemsCount(BLOOD_OF_SAINT) == 300)
			{
				htmltext = "31517-07.htm";
				st.takeItems(BLOOD_OF_SAINT, 300);
				st.rewardExpAndSp(162773, 12500);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "31517-08.htm";
		}
		else if (event.equalsIgnoreCase("reward2"))
		{
			if (st.getQuestItemsCount(BLOOD_OF_SAINT) == 300)
			{
				htmltext = "31517-07.htm";
				st.takeItems(BLOOD_OF_SAINT, 300);
				st.rewardItems(57, 100000);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "31517-08.htm";
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
				htmltext = (player.getLevel() < 60) ? "31517-02.htm" : "31517-01.htm";
				break;
			
			case STATE_STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "31517-05.htm";
				else
					htmltext = "31517-04.htm";
				break;
			
			case STATE_COMPLETED:
				htmltext = getAlreadyCompletedMsg();
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
		
		if (st.dropItems(BLOOD_OF_SAINT, 1, 300, CHANCES.get(npc.getNpcId())))
			st.set("cond", "2");
		
		return null;
	}
}