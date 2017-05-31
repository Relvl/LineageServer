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

public class Q152_ShardsOfGolem extends Quest
{
	private static final String qn = "Q152_ShardsOfGolem";
	
	// Items
	private static final int HARRIS_RECEIPT_1 = 1008;
	private static final int HARRIS_RECEIPT_2 = 1009;
	private static final int GOLEM_SHARD = 1010;
	private static final int TOOL_BOX = 1011;
	
	// Reward
	private static final int WOODEN_BREASTPLATE = 23;
	
	// NPCs
	private static final int HARRIS = 30035;
	private static final int ALTRAN = 30283;
	
	// Mob
	private static final int STONE_GOLEM = 20016;
	
	public Q152_ShardsOfGolem()
	{
		super(152, "Shards of Golem");
		
		setItemsIds(HARRIS_RECEIPT_1, HARRIS_RECEIPT_2, GOLEM_SHARD, TOOL_BOX);
		
		addStartNpc(HARRIS);
		addTalkId(HARRIS, ALTRAN);
		
		addKillId(STONE_GOLEM);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30035-02.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(HARRIS_RECEIPT_1, 1);
		}
		else if (event.equalsIgnoreCase("30283-02.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(HARRIS_RECEIPT_1, 1);
			st.giveItems(HARRIS_RECEIPT_2, 1);
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
				htmltext = (player.getLevel() < 10) ? "30035-01a.htm" : "30035-01.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case HARRIS:
						if (cond < 4)
							htmltext = "30035-03.htm";
						else if (cond == 4)
						{
							htmltext = "30035-04.htm";
							st.takeItems(HARRIS_RECEIPT_2, 1);
							st.takeItems(TOOL_BOX, 1);
							st.giveItems(WOODEN_BREASTPLATE, 1);
							st.rewardExpAndSp(5000, 0);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;
					
					case ALTRAN:
						if (cond == 1)
							htmltext = "30283-01.htm";
						else if (cond == 2)
							htmltext = "30283-03.htm";
						else if (cond == 3)
						{
							htmltext = "30283-04.htm";
							st.set("cond", "4");
							st.playSound(QuestState.SOUND_MIDDLE);
							st.takeItems(GOLEM_SHARD, -1);
							st.giveItems(TOOL_BOX, 1);
						}
						else if (cond == 4)
							htmltext = "30283-05.htm";
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
		
		if (st.dropItems(GOLEM_SHARD, 1, 5, 300000))
			st.set("cond", "3");
		
		return null;
	}
}