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

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q300_HuntingLetoLizardman extends Quest
{
	private static final String qn = "Q300_HuntingLetoLizardman";
	
	// Item
	private static final int BRACELET = 7139;
	
	// Monsters
	private static final int LETO_LIZARDMAN = 20577;
	private static final int LETO_LIZARDMAN_ARCHER = 20578;
	private static final int LETO_LIZARDMAN_SOLDIER = 20579;
	private static final int LETO_LIZARDMAN_WARRIOR = 20580;
	private static final int LETO_LIZARDMAN_OVERLORD = 20582;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	{
		CHANCES.put(LETO_LIZARDMAN, 300000);
		CHANCES.put(LETO_LIZARDMAN_ARCHER, 320000);
		CHANCES.put(LETO_LIZARDMAN_SOLDIER, 350000);
		CHANCES.put(LETO_LIZARDMAN_WARRIOR, 650000);
		CHANCES.put(LETO_LIZARDMAN_OVERLORD, 700000);
	}
	
	public Q300_HuntingLetoLizardman()
	{
		super(300, "Hunting Leto Lizardman");
		
		setItemsIds(BRACELET);
		
		addStartNpc(30126); // Rath
		addTalkId(30126);
		
		addKillId(LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER, LETO_LIZARDMAN_WARRIOR, LETO_LIZARDMAN_OVERLORD);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30126-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30126-05.htm"))
		{
			if (st.getQuestItemsCount(BRACELET) >= 60)
			{
				htmltext = "30126-06.htm";
				st.takeItems(BRACELET, -1);
				
				final int luck = Rnd.get(3);
				if (luck == 0)
					st.rewardItems(57, 30000);
				else if (luck == 1)
					st.rewardItems(1867, 50);
				else if (luck == 2)
					st.rewardItems(1872, 50);
				
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
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
				htmltext = (player.getLevel() < 34) ? "30126-01.htm" : "30126-02.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (st.getInt("cond") == 1) ? "30126-04a.htm" : "30126-04.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember == null)
			return null;
		
		QuestState st = partyMember.getQuestState(qn);
		
		if (st.dropItems(BRACELET, 1, 60, CHANCES.get(npc.getNpcId())))
			st.set("cond", "2");
		
		return null;
	}
}