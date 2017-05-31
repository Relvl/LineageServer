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

public class Q659_IdRatherBeCollectingFairyBreath extends Quest
{
	private static final String qn = "Q659_IdRatherBeCollectingFairyBreath";
	
	// NPCs
	private static final int GALATEA = 30634;
	
	// Item
	private static final int FAIRY_BREATH = 8286;
	
	// Monsters
	private static final int SOBBING_WIND = 21023;
	private static final int BABBLING_WIND = 21024;
	private static final int GIGGLING_WIND = 21025;
	
	public Q659_IdRatherBeCollectingFairyBreath()
	{
		super(659, "I'd Rather Be Collecting Fairy Breath");
		
		setItemsIds(FAIRY_BREATH);
		
		addStartNpc(GALATEA);
		addTalkId(GALATEA);
		
		addKillId(GIGGLING_WIND, BABBLING_WIND, SOBBING_WIND);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30634-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30634-06.htm"))
		{
			final int count = st.getQuestItemsCount(FAIRY_BREATH);
			if (count > 0)
			{
				st.takeItems(FAIRY_BREATH, count);
				if (count < 10)
					st.rewardItems(57, count * 50);
				else
					st.rewardItems(57, count * 50 + 5365);
			}
		}
		else if (event.equalsIgnoreCase("30634-08.htm"))
			st.exitQuest(true);
		
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
				htmltext = (player.getLevel() < 26) ? "30634-01.htm" : "30634-02.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (!st.hasQuestItems(FAIRY_BREATH)) ? "30634-04.htm" : "30634-05.htm";
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
		
		st.dropItems(FAIRY_BREATH, 1, 0, 900000);
		
		return null;
	}
}