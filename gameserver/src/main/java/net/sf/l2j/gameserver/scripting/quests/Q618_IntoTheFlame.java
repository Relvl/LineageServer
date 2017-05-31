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

public class Q618_IntoTheFlame extends Quest
{
	private static final String qn = "Q618_IntoTheFlame";
	
	// NPCs
	private static final int KLEIN = 31540;
	private static final int HILDA = 31271;
	
	// Items
	private static final int VACUALITE_ORE = 7265;
	private static final int VACUALITE = 7266;
	
	// Reward
	private static final int FLOATING_STONE = 7267;
	
	public Q618_IntoTheFlame()
	{
		super(618, "Into The Flame");
		
		setItemsIds(VACUALITE_ORE, VACUALITE);
		
		addStartNpc(KLEIN);
		addTalkId(KLEIN, HILDA);
		
		// Kookaburras, Bandersnatches, Grendels
		addKillId(21274, 21275, 21276, 21277, 21282, 21283, 21284, 21285, 21290, 21291, 21292, 21293);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31540-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31540-05.htm"))
		{
			st.takeItems(VACUALITE, 1);
			st.giveItems(FLOATING_STONE, 1);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		else if (event.equalsIgnoreCase("31271-02.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31271-05.htm"))
		{
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(VACUALITE_ORE, -1);
			st.giveItems(VACUALITE, 1);
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
				htmltext = (player.getLevel() < 60) ? "31540-01.htm" : "31540-02.htm";
				break;
			
			case STATE_STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case KLEIN:
						htmltext = (cond == 4) ? "31540-04.htm" : "31540-03.htm";
						break;
					
					case HILDA:
						if (cond == 1)
							htmltext = "31271-01.htm";
						else if (cond == 2)
							htmltext = "31271-03.htm";
						else if (cond == 3)
							htmltext = "31271-04.htm";
						else if (cond == 4)
							htmltext = "31271-06.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMember(player, npc, "2");
		if (partyMember == null)
			return null;
		
		QuestState st = partyMember.getQuestState(qn);
		
		if (st.dropItems(VACUALITE_ORE, 1, 50, 500000))
			st.set("cond", "3");
		
		return null;
	}
}