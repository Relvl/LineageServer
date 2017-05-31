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

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q646_SignsOfRevolt extends Quest
{
	private static final String qn = "Q646_SignsOfRevolt";
	
	// NPC
	private static final int TORRANT = 32016;
	
	// Item
	private static final int CURSED_DOLL = 8087;
	
	// Rewards
	private static final int[][] REWARDS =
	{
		{
			1880,
			9
		},
		{
			1881,
			12
		},
		{
			1882,
			20
		},
		{
			57,
			21600
		}
	};
	
	public Q646_SignsOfRevolt()
	{
		super(646, "Signs Of Revolt");
		
		setItemsIds(CURSED_DOLL);
		
		addStartNpc(TORRANT);
		addTalkId(TORRANT);
		
		addKillId(22029, 22030, 22031, 22032, 22033, 22034, 22035, 22036, 22037, 22038, 22039, 22040, 22041, 22042, 22043, 22044, 22045, 22047, 22049);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("32016-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (StringUtil.isDigit(event))
		{
			htmltext = "32016-07.htm";
			st.takeItems(CURSED_DOLL, -1);
			
			final int reward[] = REWARDS[Integer.parseInt(event)];
			st.giveItems(reward[0], reward[1]);
			
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
				htmltext = (player.getLevel() < 40) ? "32016-02.htm" : "32016-01.htm";
				break;
			
			case STATE_STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "32016-04.htm";
				else if (cond == 2)
					htmltext = "32016-05.htm";
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
		
		if (st.dropItems(CURSED_DOLL, 1, 180, 750000))
			st.set("cond", "2");
		
		return null;
	}
}