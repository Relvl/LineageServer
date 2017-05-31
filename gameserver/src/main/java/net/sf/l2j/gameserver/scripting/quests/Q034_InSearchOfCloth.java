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

public class Q034_InSearchOfCloth extends Quest
{
	private static final String qn = "Q034_InSearchOfCloth";
	
	// NPCs
	private static final int RADIA = 30088;
	private static final int RALFORD = 30165;
	private static final int VARAN = 30294;
	
	// Monsters
	private static final int TRISALIM_SPIDER = 20560;
	private static final int TRISALIM_TARANTULA = 20561;
	
	// Items
	private static final int SPINNERET = 7528;
	private static final int SUEDE = 1866;
	private static final int THREAD = 1868;
	private static final int SPIDERSILK = 7161;
	
	// Rewards
	private static final int MYSTERIOUS_CLOTH = 7076;
	
	public Q034_InSearchOfCloth()
	{
		super(34, "In Search of Cloth");
		
		setItemsIds(SPINNERET, SPIDERSILK);
		
		addStartNpc(RADIA);
		addTalkId(RADIA, RALFORD, VARAN);
		
		addKillId(TRISALIM_SPIDER, TRISALIM_TARANTULA);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30088-1.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30294-1.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30088-3.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30165-1.htm"))
		{
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30165-3.htm"))
		{
			st.set("cond", "6");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SPINNERET, 10);
			st.giveItems(SPIDERSILK, 1);
		}
		else if (event.equalsIgnoreCase("30088-5.htm"))
		{
			if (st.getQuestItemsCount(SUEDE) >= 3000 && st.getQuestItemsCount(THREAD) >= 5000 && st.hasQuestItems(SPIDERSILK))
			{
				st.takeItems(SPIDERSILK, 1);
				st.takeItems(SUEDE, 3000);
				st.takeItems(THREAD, 5000);
				st.giveItems(MYSTERIOUS_CLOTH, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "30088-4a.htm";
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
				if (player.getLevel() >= 60)
				{
					QuestState fwear = player.getQuestState("Q037_MakeFormalWear");
					if (fwear != null && fwear.getInt("cond") == 6)
						htmltext = "30088-0.htm";
					else
						htmltext = "30088-0a.htm";
				}
				else
					htmltext = "30088-0b.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case RADIA:
						if (cond == 1)
							htmltext = "30088-1a.htm";
						else if (cond == 2)
							htmltext = "30088-2.htm";
						else if (cond == 3)
							htmltext = "30088-3a.htm";
						else if (cond == 6)
						{
							if (st.getQuestItemsCount(SUEDE) < 3000 || st.getQuestItemsCount(THREAD) < 5000 || !st.hasQuestItems(SPIDERSILK))
								htmltext = "30088-4a.htm";
							else
								htmltext = "30088-4.htm";
						}
						break;
					
					case VARAN:
						if (cond == 1)
							htmltext = "30294-0.htm";
						else if (cond > 1)
							htmltext = "30294-1a.htm";
						break;
					
					case RALFORD:
						if (cond == 3)
							htmltext = "30165-0.htm";
						else if (cond == 4 && st.getQuestItemsCount(SPINNERET) < 10)
							htmltext = "30165-1a.htm";
						else if (cond == 5)
							htmltext = "30165-2.htm";
						else if (cond > 5)
							htmltext = "30165-3a.htm";
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
		QuestState st = checkPlayerCondition(player, npc, "cond", "4");
		if (st == null)
			return null;
		
		if (st.dropItems(SPINNERET, 1, 10, 500000))
			st.set("cond", "5");
		
		return null;
	}
}