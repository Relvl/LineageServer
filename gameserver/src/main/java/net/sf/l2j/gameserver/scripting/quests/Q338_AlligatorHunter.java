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

public class Q338_AlligatorHunter extends Quest
{
	private static final String qn = "Q338_AlligatorHunter";
	
	// Item
	private static final int ALLIGATOR_PELT = 4337;
	
	public Q338_AlligatorHunter()
	{
		super(338, "Alligator Hunter");
		
		setItemsIds(ALLIGATOR_PELT);
		
		addStartNpc(30892); // Enverun
		addTalkId(30892);
		
		addKillId(20135); // Alligator
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30892-02.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30892-05.htm"))
		{
			final int pelts = st.getQuestItemsCount(ALLIGATOR_PELT);
			
			int reward = pelts * 60;
			if (pelts > 10)
				reward += 3430;
			
			st.takeItems(ALLIGATOR_PELT, -1);
			st.rewardItems(57, reward);
		}
		else if (event.equalsIgnoreCase("30892-08.htm"))
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
				htmltext = (player.getLevel() < 40) ? "30892-00.htm" : "30892-01.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (st.hasQuestItems(ALLIGATOR_PELT)) ? "30892-03.htm" : "30892-04.htm";
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
		
		st.dropItemsAlways(ALLIGATOR_PELT, 1, 0);
		
		return null;
	}
}