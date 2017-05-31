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

/**
 * The onKill section of that quest is directly written on Q605.
 */
public class Q606_WarWithVarkaSilenos extends Quest
{
	private static final String qn = "Q606_WarWithVarkaSilenos";
	
	// Items
	private static final int HORN_OF_BUFFALO = 7186;
	private static final int VARKA_MANE = 7233;
	
	public Q606_WarWithVarkaSilenos()
	{
		super(606, "War with Varka Silenos");
		
		setItemsIds(VARKA_MANE);
		
		addStartNpc(31370); // Kadun Zu Ketra
		addTalkId(31370);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31370-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31370-07.htm"))
		{
			if (st.getQuestItemsCount(VARKA_MANE) >= 100)
			{
				st.playSound(QuestState.SOUND_ITEMGET);
				st.takeItems(VARKA_MANE, 100);
				st.giveItems(HORN_OF_BUFFALO, 20);
			}
			else
				htmltext = "31370-08.htm";
		}
		else if (event.equalsIgnoreCase("31370-09.htm"))
		{
			st.takeItems(VARKA_MANE, -1);
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
				htmltext = (player.getLevel() >= 74 && player.isAlliedWithKetra()) ? "31370-01.htm" : "31370-02.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (st.hasQuestItems(VARKA_MANE)) ? "31370-04.htm" : "31370-05.htm";
				break;
		}
		
		return htmltext;
	}
}