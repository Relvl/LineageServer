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
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q360_PlunderTheirSupplies extends Quest
{
	private static final String qn = "Q360_PlunderTheirSupplies";
	
	// Items
	private static final int SUPPLY_ITEM = 5872;
	private static final int SUSPICIOUS_DOCUMENT = 5871;
	private static final int RECIPE_OF_SUPPLY = 5870;
	
	private static final int[][][] DROPLIST =
	{
		{
			{
				SUSPICIOUS_DOCUMENT,
				1,
				0,
				50000
			},
			{
				SUPPLY_ITEM,
				1,
				0,
				500000
			}
		},
		{
			{
				SUSPICIOUS_DOCUMENT,
				1,
				0,
				50000
			},
			{
				SUPPLY_ITEM,
				1,
				0,
				660000
			}
		}
	};
	
	public Q360_PlunderTheirSupplies()
	{
		super(360, "Plunder Their Supplies");
		
		setItemsIds(RECIPE_OF_SUPPLY, SUPPLY_ITEM, SUSPICIOUS_DOCUMENT);
		
		addStartNpc(30873); // Coleman
		addTalkId(30873);
		
		addKillId(20666, 20669);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30873-2.htm"))
		{
			st.setState(QuestState.STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30873-6.htm"))
		{
			st.takeItems(SUPPLY_ITEM, -1);
			st.takeItems(SUSPICIOUS_DOCUMENT, -1);
			st.takeItems(RECIPE_OF_SUPPLY, -1);
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
				htmltext = (player.getLevel() < 52) ? "30873-0a.htm" : "30873-0.htm";
				break;
			
			case QuestState.STATE_STARTED:
				final int supplyItems = st.getQuestItemsCount(SUPPLY_ITEM);
				if (supplyItems == 0)
					htmltext = "30873-3.htm";
				else
				{
					final int reward = 6000 + (supplyItems * 100) + (st.getQuestItemsCount(RECIPE_OF_SUPPLY) * 6000);
					
					htmltext = "30873-5.htm";
					st.takeItems(SUPPLY_ITEM, -1);
					st.takeItems(RECIPE_OF_SUPPLY, -1);
					st.rewardItems(ItemConst.ADENA_ID, reward);
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = checkPlayerState(player, npc, QuestState.STATE_STARTED);
		if (st == null)
			return null;
		
		st.dropMultipleItems(DROPLIST[(npc.getNpcId() == 20666) ? 0 : 1]);
		
		if (st.getQuestItemsCount(SUSPICIOUS_DOCUMENT) == 5)
		{
			st.takeItems(SUSPICIOUS_DOCUMENT, 5);
			st.giveItems(RECIPE_OF_SUPPLY, 1);
		}
		
		return null;
	}
}