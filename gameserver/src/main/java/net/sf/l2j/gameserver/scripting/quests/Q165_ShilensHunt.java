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
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q165_ShilensHunt extends Quest
{
	private static final String qn = "Q165_ShilensHunt";
	
	// Monsters
	private static final int ASHEN_WOLF = 20456;
	private static final int YOUNG_BROWN_KELTIR = 20529;
	private static final int BROWN_KELTIR = 20532;
	private static final int ELDER_BROWN_KELTIR = 20536;
	
	// Items
	private static final int DARK_BEZOAR = 1160;
	private static final int LESSER_HEALING_POTION = 1060;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	{
		CHANCES.put(ASHEN_WOLF, 1000000);
		CHANCES.put(YOUNG_BROWN_KELTIR, 333333);
		CHANCES.put(BROWN_KELTIR, 333333);
		CHANCES.put(ELDER_BROWN_KELTIR, 666667);
	}
	
	public Q165_ShilensHunt()
	{
		super(165, "Shilen's Hunt");
		
		setItemsIds(DARK_BEZOAR);
		
		addStartNpc(30348); // Nelsya
		addTalkId(30348);
		
		addKillId(ASHEN_WOLF, YOUNG_BROWN_KELTIR, BROWN_KELTIR, ELDER_BROWN_KELTIR);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30348-03.htm"))
		{
			st.setState(STATE_STARTED);
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
			case STATE_CREATED:
				if (player.getRace() != Race.DarkElf)
					htmltext = "30348-00.htm";
				else if (player.getLevel() < 3)
					htmltext = "30348-01.htm";
				else
					htmltext = "30348-02.htm";
				break;
			
			case STATE_STARTED:
				if (st.getQuestItemsCount(DARK_BEZOAR) >= 13)
				{
					htmltext = "30348-05.htm";
					st.takeItems(DARK_BEZOAR, -1);
					st.rewardItems(LESSER_HEALING_POTION, 5);
					st.rewardExpAndSp(1000, 0);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(false);
				}
				else
					htmltext = "30348-04.htm";
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
		
		if (st.dropItems(DARK_BEZOAR, 1, 13, CHANCES.get(npc.getNpcId())))
			st.set("cond", "2");
		
		return null;
	}
}