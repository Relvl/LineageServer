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

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q258_BringWolfPelts extends Quest
{
	private static final String qn = "Q258_BringWolfPelts";
	
	// Item
	private static final int WOLF_PELT = 702;
	
	// Rewards
	private static final int COTTON_SHIRT = 390;
	private static final int LEATHER_PANTS = 29;
	private static final int LEATHER_SHIRT = 22;
	private static final int SHORT_LEATHER_GLOVES = 1119;
	private static final int TUNIC = 426;
	
	public Q258_BringWolfPelts()
	{
		super(258, "Bring Wolf Pelts");
		
		setItemsIds(WOLF_PELT);
		
		addStartNpc(30001); // Lector
		addTalkId(30001);
		
		addKillId(20120, 20442); // Wolf, Elder Wolf
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30001-03.htm"))
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
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;
		
		switch (st.getState())
		{
			case STATE_CREATED:
				htmltext = (player.getLevel() < 3) ? "30001-01.htm" : "30001-02.htm";
				break;
			
			case STATE_STARTED:
				if (st.getQuestItemsCount(WOLF_PELT) < 40)
					htmltext = "30001-05.htm";
				else
				{
					st.takeItems(WOLF_PELT, -1);
					int randomNumber = Rnd.get(16);
					
					// Reward is based on a random number (1D16).
					if (randomNumber == 0)
						st.giveItems(COTTON_SHIRT, 1);
					else if (randomNumber < 6)
						st.giveItems(LEATHER_PANTS, 1);
					else if (randomNumber < 9)
						st.giveItems(LEATHER_SHIRT, 1);
					else if (randomNumber < 13)
						st.giveItems(SHORT_LEATHER_GLOVES, 1);
					else
						st.giveItems(TUNIC, 1);
					
					htmltext = "30001-06.htm";
					
					if (randomNumber == 0)
						st.playSound(QuestState.SOUND_JACKPOT);
					else
						st.playSound(QuestState.SOUND_FINISH);
					
					st.exitQuest(true);
				}
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
		
		if (st.dropItemsAlways(WOLF_PELT, 1, 40))
			st.set("cond", "2");
		
		return null;
	}
}