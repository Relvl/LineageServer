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

public class Q296_TarantulasSpiderSilk extends Quest
{
	private static final String qn = "Q296_TarantulasSpiderSilk";
	
	// NPCs
	private static final int MION = 30519;
	private static final int DEFENDER_NATHAN = 30548;
	
	// Quest Items
	private static final int TARANTULA_SPIDER_SILK = 1493;
	private static final int TARANTULA_SPINNERETTE = 1494;
	
	// Items
	private static final int RING_OF_RACCOON = 1508;
	private static final int RING_OF_FIREFLY = 1509;
	
	public Q296_TarantulasSpiderSilk()
	{
		super(296, "Tarantula's Spider Silk");
		
		setItemsIds(TARANTULA_SPIDER_SILK, TARANTULA_SPINNERETTE);
		
		addStartNpc(MION);
		addTalkId(MION, DEFENDER_NATHAN);
		
		addKillId(20394, 20403, 20508); // Crimson Tarantula, Hunter Tarantula, Plunder arantula
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30519-03.htm"))
		{
			if (st.hasAtLeastOneQuestItem(RING_OF_RACCOON, RING_OF_FIREFLY))
			{
				st.setState(STATE_STARTED);
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
			}
			else
				htmltext = "30519-03a.htm";
		}
		else if (event.equalsIgnoreCase("30519-06.htm"))
		{
			st.takeItems(TARANTULA_SPIDER_SILK, -1);
			st.takeItems(TARANTULA_SPINNERETTE, -1);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		else if (event.equalsIgnoreCase("30548-02.htm"))
		{
			final int count = st.getQuestItemsCount(TARANTULA_SPINNERETTE);
			if (count > 0)
			{
				htmltext = "30548-03.htm";
				st.takeItems(TARANTULA_SPINNERETTE, -1);
				st.giveItems(TARANTULA_SPIDER_SILK, count * (15 + Rnd.get(10)));
			}
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
				htmltext = (player.getLevel() < 15) ? "30519-01.htm" : "30519-02.htm";
				break;
			
			case STATE_STARTED:
				switch (npc.getNpcId())
				{
					case MION:
						final int count = st.getQuestItemsCount(TARANTULA_SPIDER_SILK);
						if (count == 0)
							htmltext = "30519-04.htm";
						else
						{
							htmltext = "30519-05.htm";
							st.takeItems(TARANTULA_SPIDER_SILK, -1);
							st.rewardItems(57, ((count >= 10) ? 2000 : 0) + count * 30);
						}
						break;
					
					case DEFENDER_NATHAN:
						htmltext = "30548-01.htm";
						break;
				}
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
		
		final int rnd = Rnd.get(100);
		if (rnd > 95)
			st.dropItemsAlways(TARANTULA_SPINNERETTE, 1, 0);
		else if (rnd > 45)
			st.dropItemsAlways(TARANTULA_SPIDER_SILK, 1, 0);
		
		return null;
	}
}