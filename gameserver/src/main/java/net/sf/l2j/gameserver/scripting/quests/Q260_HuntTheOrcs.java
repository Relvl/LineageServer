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
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q260_HuntTheOrcs extends Quest
{
	private static final String qn = "Q260_HuntTheOrcs";
	
	// NPC
	private static final int RAYEN = 30221;
	
	// Items
	private static final int ORC_AMULET = 1114;
	private static final int ORC_NECKLACE = 1115;
	
	// Monsters
	private static final int KABOO_ORC = 20468;
	private static final int KABOO_ORC_ARCHER = 20469;
	private static final int KABOO_ORC_GRUNT = 20470;
	private static final int KABOO_ORC_FIGHTER = 20471;
	private static final int KABOO_ORC_FIGHTER_LEADER = 20472;
	private static final int KABOO_ORC_FIGHTER_LIEUTENANT = 20473;
	
	public Q260_HuntTheOrcs()
	{
		super(260, "Hunt the Orcs");
		
		setItemsIds(ORC_AMULET, ORC_NECKLACE);
		
		addStartNpc(RAYEN);
		addTalkId(RAYEN);
		
		addKillId(KABOO_ORC, KABOO_ORC_ARCHER, KABOO_ORC_GRUNT, KABOO_ORC_FIGHTER, KABOO_ORC_FIGHTER_LEADER, KABOO_ORC_FIGHTER_LIEUTENANT);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30221-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30221-06.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getRace() != PlayerRace.Elf)
					htmltext = "30221-00.htm";
				else if (player.getLevel() < 6)
					htmltext = "30221-01.htm";
				else
					htmltext = "30221-02.htm";
				break;
			
			case STATE_STARTED:
				int amulet = st.getQuestItemsCount(ORC_AMULET);
				int necklace = st.getQuestItemsCount(ORC_NECKLACE);
				
				if (amulet == 0 && necklace == 0)
					htmltext = "30221-04.htm";
				else
				{
					htmltext = "30221-05.htm";
					st.takeItems(ORC_AMULET, -1);
					st.takeItems(ORC_NECKLACE, -1);
					st.rewardItems(ItemConst.ADENA_ID, amulet * 5 + necklace * 15);
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
		
		switch (npc.getNpcId())
		{
			case KABOO_ORC:
			case KABOO_ORC_GRUNT:
			case KABOO_ORC_ARCHER:
				st.dropItems(ORC_AMULET, 1, 0, 500000);
				break;
			
			case KABOO_ORC_FIGHTER:
			case KABOO_ORC_FIGHTER_LEADER:
			case KABOO_ORC_FIGHTER_LIEUTENANT:
				st.dropItems(ORC_NECKLACE, 1, 0, 500000);
				break;
		}
		
		return null;
	}
}