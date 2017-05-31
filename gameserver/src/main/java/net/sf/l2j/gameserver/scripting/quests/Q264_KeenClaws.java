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

public class Q264_KeenClaws extends Quest
{
	private static final String qn = "Q264_KeenClaws";
	
	// Item
	private static final int WOLF_CLAW = 1367;
	
	// Rewards
	private static final int LEATHER_SANDALS = 36;
	private static final int WOODEN_HELMET = 43;
	private static final int STOCKINGS = 462;
	private static final int HEALING_POTION = 1061;
	private static final int SHORT_GLOVES = 48;
	private static final int CLOTH_SHOES = 35;
	
	public Q264_KeenClaws()
	{
		super(264, "Keen Claws");
		
		setItemsIds(WOLF_CLAW);
		
		addStartNpc(30136); // Payne
		addTalkId(30136);
		
		addKillId(20003, 20456); // Goblin, Wolf
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30136-03.htm"))
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
				htmltext = (player.getLevel() < 3) ? "30136-01.htm" : "30136-02.htm";
				break;
			
			case STATE_STARTED:
				final int count = st.getQuestItemsCount(WOLF_CLAW);
				if (count < 50)
					htmltext = "30136-04.htm";
				else
				{
					htmltext = "30136-05.htm";
					st.takeItems(WOLF_CLAW, -1);
					
					final int n = Rnd.get(17);
					if (n == 0)
					{
						st.giveItems(WOODEN_HELMET, 1);
						st.playSound(QuestState.SOUND_JACKPOT);
					}
					else if (n < 2)
						st.giveItems(57, 1000);
					else if (n < 5)
						st.giveItems(LEATHER_SANDALS, 1);
					else if (n < 8)
					{
						st.giveItems(STOCKINGS, 1);
						st.giveItems(57, 50);
					}
					else if (n < 11)
						st.giveItems(HEALING_POTION, 1);
					else if (n < 14)
						st.giveItems(SHORT_GLOVES, 1);
					else
						st.giveItems(CLOTH_SHOES, 1);
					
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
		
		if (npc.getNpcId() == 20003)
		{
			if (st.dropItems(WOLF_CLAW, Rnd.nextBoolean() ? 2 : 4, 50, 500000))
				st.set("cond", "2");
		}
		else if (st.dropItemsAlways(WOLF_CLAW, (Rnd.get(5) < 4) ? 1 : 2, 50))
			st.set("cond", "2");
		
		return null;
	}
}