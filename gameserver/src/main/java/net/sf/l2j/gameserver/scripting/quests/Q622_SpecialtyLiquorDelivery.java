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
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q622_SpecialtyLiquorDelivery extends Quest
{
	private static final String qn = "Q622_SpecialtyLiquorDelivery";
	
	// Items
	private static final int SPECIAL_DRINK = 7197;
	private static final int FEE_OF_SPECIAL_DRINK = 7198;
	
	// NPCs
	private static final int JEREMY = 31521;
	private static final int PULIN = 31543;
	private static final int NAFF = 31544;
	private static final int CROCUS = 31545;
	private static final int KUBER = 31546;
	private static final int BEOLIN = 31547;
	private static final int LIETTA = 31267;
	
	// Rewards
	private static final int HASTE_POTION = 1062;
	private static final int[] RECIPES =
	{
		6847,
		6849,
		6851
	};
	
	public Q622_SpecialtyLiquorDelivery()
	{
		super(622, "Specialty Liquor Delivery");
		
		setItemsIds(SPECIAL_DRINK, FEE_OF_SPECIAL_DRINK);
		
		addStartNpc(JEREMY);
		addTalkId(JEREMY, PULIN, NAFF, CROCUS, KUBER, BEOLIN, LIETTA);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31521-02.htm"))
		{
			st.setState(QuestState.STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(SPECIAL_DRINK, 5);
		}
		else if (event.equalsIgnoreCase("31547-02.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SPECIAL_DRINK, 1);
			st.giveItems(FEE_OF_SPECIAL_DRINK, 1);
		}
		else if (event.equalsIgnoreCase("31546-02.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SPECIAL_DRINK, 1);
			st.giveItems(FEE_OF_SPECIAL_DRINK, 1);
		}
		else if (event.equalsIgnoreCase("31545-02.htm"))
		{
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SPECIAL_DRINK, 1);
			st.giveItems(FEE_OF_SPECIAL_DRINK, 1);
		}
		else if (event.equalsIgnoreCase("31544-02.htm"))
		{
			st.set("cond", "5");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SPECIAL_DRINK, 1);
			st.giveItems(FEE_OF_SPECIAL_DRINK, 1);
		}
		else if (event.equalsIgnoreCase("31543-02.htm"))
		{
			st.set("cond", "6");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SPECIAL_DRINK, 1);
			st.giveItems(FEE_OF_SPECIAL_DRINK, 1);
		}
		else if (event.equalsIgnoreCase("31521-06.htm"))
		{
			st.set("cond", "7");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(FEE_OF_SPECIAL_DRINK, 5);
		}
		else if (event.equalsIgnoreCase("31267-02.htm"))
		{
			if (Rnd.get(5) < 1)
				st.giveItems(RECIPES[Rnd.get(RECIPES.length)], 1);
			else
			{
				st.rewardItems(ItemConst.ADENA_ID, 18800);
				st.rewardItems(HASTE_POTION, 1);
			}
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
			case QuestState.STATE_CREATED:
				htmltext = (player.getLevel() < 68) ? "31521-03.htm" : "31521-01.htm";
				break;
			
			case QuestState.STATE_STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case JEREMY:
						if (cond < 6)
							htmltext = "31521-04.htm";
						else if (cond == 6)
							htmltext = "31521-05.htm";
						else if (cond == 7)
							htmltext = "31521-06.htm";
						break;
					
					case BEOLIN:
						if (cond == 1 && st.getQuestItemsCount(SPECIAL_DRINK) == 5)
							htmltext = "31547-01.htm";
						else if (cond > 1)
							htmltext = "31547-03.htm";
						break;
					
					case KUBER:
						if (cond == 2 && st.getQuestItemsCount(SPECIAL_DRINK) == 4)
							htmltext = "31546-01.htm";
						else if (cond > 2)
							htmltext = "31546-03.htm";
						break;
					
					case CROCUS:
						if (cond == 3 && st.getQuestItemsCount(SPECIAL_DRINK) == 3)
							htmltext = "31545-01.htm";
						else if (cond > 3)
							htmltext = "31545-03.htm";
						break;
					
					case NAFF:
						if (cond == 4 && st.getQuestItemsCount(SPECIAL_DRINK) == 2)
							htmltext = "31544-01.htm";
						else if (cond > 4)
							htmltext = "31544-03.htm";
						break;
					
					case PULIN:
						if (cond == 5 && st.getQuestItemsCount(SPECIAL_DRINK) == 1)
							htmltext = "31543-01.htm";
						else if (cond > 5)
							htmltext = "31543-03.htm";
						break;
					
					case LIETTA:
						if (cond == 7)
							htmltext = "31267-01.htm";
						break;
				}
		}
		
		return htmltext;
	}
}