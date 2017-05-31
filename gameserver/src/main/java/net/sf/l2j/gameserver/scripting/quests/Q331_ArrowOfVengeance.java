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

public class Q331_ArrowOfVengeance extends Quest
{
	private static final String qn = "Q331_ArrowOfVengeance";
	
	// Items
	private static final int HARPY_FEATHER = 1452;
	private static final int MEDUSA_VENOM = 1453;
	private static final int WYRM_TOOTH = 1454;
	
	public Q331_ArrowOfVengeance()
	{
		super(331, "Arrow Of Vengeance");
		
		setItemsIds(HARPY_FEATHER, MEDUSA_VENOM, WYRM_TOOTH);
		
		addStartNpc(30125); // Belton
		addTalkId(30125);
		
		addKillId(20145, 20158, 20176);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30125-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30125-06.htm"))
		{
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
			case STATE_CREATED:
				htmltext = (player.getLevel() < 32) ? "30125-01.htm" : "30125-02.htm";
				break;
			
			case STATE_STARTED:
				final int harpyFeather = st.getQuestItemsCount(HARPY_FEATHER);
				final int medusaVenom = st.getQuestItemsCount(MEDUSA_VENOM);
				final int wyrmTooth = st.getQuestItemsCount(WYRM_TOOTH);
				
				if (harpyFeather + medusaVenom + wyrmTooth > 0)
				{
					htmltext = "30125-05.htm";
					st.takeItems(HARPY_FEATHER, -1);
					st.takeItems(MEDUSA_VENOM, -1);
					st.takeItems(WYRM_TOOTH, -1);
					
					int reward = harpyFeather * 78 + medusaVenom * 88 + wyrmTooth * 92;
					if (harpyFeather + medusaVenom + wyrmTooth > 10)
						reward += 3100;
					
					st.rewardItems(57, reward);
				}
				else
					htmltext = "30125-04.htm";
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
		
		switch (npc.getNpcId())
		{
			case 20145:
				st.dropItems(HARPY_FEATHER, 1, 0, 500000);
				break;
			
			case 20158:
				st.dropItems(MEDUSA_VENOM, 1, 0, 500000);
				break;
			
			case 20176:
				st.dropItems(WYRM_TOOTH, 1, 0, 500000);
				break;
		}
		
		return null;
	}
}