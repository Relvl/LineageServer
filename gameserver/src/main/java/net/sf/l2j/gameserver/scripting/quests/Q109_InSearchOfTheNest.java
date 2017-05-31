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

public class Q109_InSearchOfTheNest extends Quest
{
	private static final String qn = "Q109_InSearchOfTheNest";
	
	// NPCs
	private static final int PIERCE = 31553;
	private static final int KAHMAN = 31554;
	private static final int SCOUT_CORPSE = 32015;
	
	// Items
	private static final int SCOUT_MEMO = 8083;
	private static final int RECRUIT_BADGE = 7246;
	private static final int SOLDIER_BADGE = 7247;
	
	public Q109_InSearchOfTheNest()
	{
		super(109, "In Search of the Nest");
		
		setItemsIds(SCOUT_MEMO);
		
		addStartNpc(PIERCE);
		addTalkId(PIERCE, SCOUT_CORPSE, KAHMAN);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31553-01.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("32015-02.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.giveItems(SCOUT_MEMO, 1);
		}
		else if (event.equalsIgnoreCase("31553-03.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SCOUT_MEMO, 1);
		}
		else if (event.equalsIgnoreCase("31554-02.htm"))
		{
			st.rewardItems(57, 5168);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				// Must worn one or other Golden Ram Badge in order to be accepted.
				if (player.getLevel() >= 66 && st.hasAtLeastOneQuestItem(RECRUIT_BADGE, SOLDIER_BADGE))
					htmltext = "31553-00.htm";
				else
					htmltext = "31553-00a.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case PIERCE:
						if (cond == 1)
							htmltext = "31553-01a.htm";
						else if (cond == 2)
							htmltext = "31553-02.htm";
						else if (cond == 3)
							htmltext = "31553-03.htm";
						break;
					
					case SCOUT_CORPSE:
						if (cond == 1)
							htmltext = "32015-01.htm";
						else if (cond == 2)
							htmltext = "32015-02.htm";
						break;
					
					case KAHMAN:
						if (cond == 3)
							htmltext = "31554-01.htm";
						break;
				}
				break;
			
			case STATE_COMPLETED:
				htmltext = getAlreadyCompletedMsg();
				break;
		}
		
		return htmltext;
	}
}