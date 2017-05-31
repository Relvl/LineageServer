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

public class Q053_LinnaeusSpecialBait extends Quest
{
	private static final String qn = "Q053_LinnaeusSpecialBait";
	
	// Item
	private static final int CRIMSON_DRAKE_HEART = 7624;
	
	// Reward
	private static final int FLAMING_FISHING_LURE = 7613;
	
	public Q053_LinnaeusSpecialBait()
	{
		super(53, "Linnaues' Special Bait");
		
		setItemsIds(CRIMSON_DRAKE_HEART);
		
		addStartNpc(31577); // Linnaeus
		addTalkId(31577);
		
		addKillId(20670); // Crimson Drake
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31577-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31577-07.htm"))
		{
			htmltext = "31577-06.htm";
			st.takeItems(CRIMSON_DRAKE_HEART, -1);
			st.rewardItems(FLAMING_FISHING_LURE, 4);
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
				htmltext = (player.getLevel() < 60) ? "31577-02.htm" : "31577-01.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (st.getQuestItemsCount(CRIMSON_DRAKE_HEART) == 100) ? "31577-04.htm" : "31577-05.htm";
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
		
		if (st.dropItems(CRIMSON_DRAKE_HEART, 1, 100, 500000))
			st.set("cond", "2");
		
		return null;
	}
}