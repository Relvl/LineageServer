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

public class Q632_NecromancersRequest extends Quest
{
	private static final String qn = "Q632_NecromancersRequest";
	
	// Monsters
	private static final int[] VAMPIRES =
	{
		21568,
		21573,
		21582,
		21585,
		21586,
		21587,
		21588,
		21589,
		21590,
		21591,
		21592,
		21593,
		21594,
		21595
	};
	
	private static final int[] UNDEADS =
	{
		21547,
		21548,
		21549,
		21551,
		21552,
		21555,
		21556,
		21562,
		21571,
		21576,
		21577,
		21579
	};
	
	// Items
	private static final int VAMPIRE_HEART = 7542;
	private static final int ZOMBIE_BRAIN = 7543;
	
	public Q632_NecromancersRequest()
	{
		super(632, "Necromancer's Request");
		
		setItemsIds(VAMPIRE_HEART, ZOMBIE_BRAIN);
		
		addStartNpc(31522); // Mysterious Wizard
		addTalkId(31522);
		
		addKillId(VAMPIRES);
		addKillId(UNDEADS);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31522-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31522-06.htm"))
		{
			if (st.getQuestItemsCount(VAMPIRE_HEART) >= 200)
			{
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_MIDDLE);
				st.takeItems(VAMPIRE_HEART, -1);
				st.rewardItems(57, 120000);
			}
			else
				htmltext = "31522-09.htm";
		}
		else if (event.equalsIgnoreCase("31522-08.htm"))
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
				htmltext = (player.getLevel() < 63) ? "31522-01.htm" : "31522-02.htm";
				break;
			
			case STATE_STARTED:
				htmltext = (st.getQuestItemsCount(VAMPIRE_HEART) >= 200) ? "31522-05.htm" : "31522-04.htm";
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
		
		for (int undead : UNDEADS)
		{
			if (undead == npc.getNpcId())
			{
				st.dropItems(ZOMBIE_BRAIN, 1, 0, 330000);
				return null;
			}
		}
		
		if (st.getInt("cond") == 1 && st.dropItems(VAMPIRE_HEART, 1, 200, 500000))
			st.set("cond", "2");
		
		return null;
	}
}