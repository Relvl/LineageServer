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

public class Q051_OFullesSpecialBait extends Quest
{
	private static final String qn = "Q051_OFullesSpecialBait";
	
	// Item
	private static final int LOST_BAIT = 7622;
	
	// Reward
	private static final int ICY_AIR_LURE = 7611;
	
	public Q051_OFullesSpecialBait()
	{
		super(51, "O'Fulle's Special Bait");
		
		setItemsIds(LOST_BAIT);
		
		addStartNpc(31572); // O'Fulle
		addTalkId(31572);
		
		addKillId(20552); // Fettered Soul
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31572-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31572-07.htm"))
		{
			htmltext = "31572-06.htm";
			st.takeItems(LOST_BAIT, -1);
			st.rewardItems(ICY_AIR_LURE, 4);
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
				htmltext = (player.getLevel() < 36) ? "31572-02.htm" : "31572-01.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (st.getQuestItemsCount(LOST_BAIT) == 100) ? "31572-04.htm" : "31572-05.htm";
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
		
		if (st.dropItemsAlways(LOST_BAIT, 1, 100))
			st.set("cond", "2");
		
		return null;
	}
}