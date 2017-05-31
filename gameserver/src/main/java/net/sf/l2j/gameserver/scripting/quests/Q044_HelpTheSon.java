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

public class Q044_HelpTheSon extends Quest
{
	private static final String qn = "Q044_HelpTheSon";
	
	// Npcs
	private static final int LUNDY = 30827;
	private static final int DRIKUS = 30505;
	
	// Items
	private static final int WORK_HAMMER = 168;
	private static final int GEMSTONE_FRAGMENT = 7552;
	private static final int GEMSTONE = 7553;
	private static final int PET_TICKET = 7585;
	
	// Monsters
	private static final int MAILLE = 20919;
	private static final int MAILLE_SCOUT = 20920;
	private static final int MAILLE_GUARD = 20921;
	
	public Q044_HelpTheSon()
	{
		super(44, "Help the Son!");
		
		setItemsIds(GEMSTONE_FRAGMENT, GEMSTONE);
		
		addStartNpc(LUNDY);
		addTalkId(LUNDY, DRIKUS);
		
		addKillId(MAILLE, MAILLE_SCOUT, MAILLE_GUARD);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30827-01.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30827-03.htm") && st.hasQuestItems(WORK_HAMMER))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(WORK_HAMMER, 1);
		}
		else if (event.equalsIgnoreCase("30827-05.htm"))
		{
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(GEMSTONE_FRAGMENT, 30);
			st.giveItems(GEMSTONE, 1);
		}
		else if (event.equalsIgnoreCase("30505-06.htm"))
		{
			st.set("cond", "5");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(GEMSTONE, 1);
		}
		else if (event.equalsIgnoreCase("30827-07.htm"))
		{
			st.giveItems(PET_TICKET, 1);
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
				htmltext = (player.getLevel() < 24) ? "30827-00a.htm" : "30827-00.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case LUNDY:
						if (cond == 1)
							htmltext = (!st.hasQuestItems(WORK_HAMMER)) ? "30827-01a.htm" : "30827-02.htm";
						else if (cond == 2)
							htmltext = "30827-03a.htm";
						else if (cond == 3)
							htmltext = "30827-04.htm";
						else if (cond == 4)
							htmltext = "30827-05a.htm";
						else if (cond == 5)
							htmltext = "30827-06.htm";
						break;
					
					case DRIKUS:
						if (cond == 4)
							htmltext = "30505-05.htm";
						else if (cond == 5)
							htmltext = "30505-06a.htm";
						break;
				}
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
		QuestState st = checkPlayerCondition(player, npc, "cond", "2");
		if (st == null)
			return null;
		
		if (st.dropItemsAlways(GEMSTONE_FRAGMENT, 1, 30))
			st.set("cond", "3");
		
		return null;
	}
}