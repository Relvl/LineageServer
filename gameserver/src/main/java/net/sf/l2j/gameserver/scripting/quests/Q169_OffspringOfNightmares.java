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

public class Q169_OffspringOfNightmares extends Quest
{
	private static final String qn = "Q169_OffspringOfNightmares";
	
	// Items
	private static final int CRACKED_SKULL = 1030;
	private static final int PERFECT_SKULL = 1031;
	private static final int BONE_GAITERS = 31;
	
	public Q169_OffspringOfNightmares()
	{
		super(169, "Offspring of Nightmares");
		
		setItemsIds(CRACKED_SKULL, PERFECT_SKULL);
		
		addStartNpc(30145); // Vlasty
		addTalkId(30145);
		
		addKillId(20105, 20025);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30145-04.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30145-08.htm"))
		{
			int reward = 17000 + (st.getQuestItemsCount(CRACKED_SKULL) * 20);
			st.takeItems(PERFECT_SKULL, -1);
			st.takeItems(CRACKED_SKULL, -1);
			st.giveItems(BONE_GAITERS, 1);
			st.rewardItems(ItemConst.ADENA_ID, reward);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getRace() != PlayerRace.DarkElf)
					htmltext = "30145-00.htm";
				else if (player.getLevel() < 15)
					htmltext = "30145-02.htm";
				else
					htmltext = "30145-03.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
				{
					if (st.hasQuestItems(CRACKED_SKULL))
						htmltext = "30145-06.htm";
					else
						htmltext = "30145-05.htm";
				}
				else if (cond == 2)
					htmltext = "30145-07.htm";
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
		QuestState st = checkPlayerState(player, npc, STATE_STARTED);
		if (st == null)
			return null;
		
		if (st.getInt("cond") == 1 && st.dropItems(PERFECT_SKULL, 1, 1, 200000))
			st.set("cond", "2");
		else
			st.dropItems(CRACKED_SKULL, 1, 0, 500000);
		
		return null;
	}
}