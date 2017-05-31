/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.scripting.quests;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q688_DefeatTheElrokianRaiders extends Quest
{
	private static final String qn = "Q688_DefeatTheElrokianRaiders";
	
	// Item
	private static final int DINOSAUR_FANG_NECKLACE = 8785;
	
	// NPC
	private static final int DINN = 32105;
	
	// Monster
	private static final int ELROKI = 22214;
	
	public Q688_DefeatTheElrokianRaiders()
	{
		super(688, "Defeat the Elrokian Raiders!");
		
		setItemsIds(DINOSAUR_FANG_NECKLACE);
		
		addStartNpc(DINN);
		addTalkId(DINN);
		
		addKillId(ELROKI);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("32105-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("32105-08.htm"))
		{
			final int count = st.getQuestItemsCount(DINOSAUR_FANG_NECKLACE);
			if (count > 0)
			{
				st.takeItems(DINOSAUR_FANG_NECKLACE, -1);
				st.rewardItems(57, count * 3000);
			}
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		else if (event.equalsIgnoreCase("32105-06.htm"))
		{
			final int count = st.getQuestItemsCount(DINOSAUR_FANG_NECKLACE);
			
			st.takeItems(DINOSAUR_FANG_NECKLACE, -1);
			st.rewardItems(57, count * 3000);
		}
		else if (event.equalsIgnoreCase("32105-07.htm"))
		{
			final int count = st.getQuestItemsCount(DINOSAUR_FANG_NECKLACE);
			if (count >= 100)
			{
				st.takeItems(DINOSAUR_FANG_NECKLACE, 100);
				st.rewardItems(57, 450000);
			}
			else
				htmltext = "32105-04.htm";
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
				htmltext = (player.getLevel() < 75) ? "32105-00.htm" : "32105-01.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (!st.hasQuestItems(DINOSAUR_FANG_NECKLACE)) ? "32105-04.htm" : "32105-05.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, STATE_STARTED);
		if (partyMember == null)
			return null;
		
		QuestState st = partyMember.getQuestState(qn);
		
		st.dropItems(DINOSAUR_FANG_NECKLACE, 1, 0, 500000);
		
		return null;
	}
}