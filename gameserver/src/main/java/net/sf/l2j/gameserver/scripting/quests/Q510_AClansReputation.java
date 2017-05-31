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

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q510_AClansReputation extends Quest
{
	private static final String qn = "Q510_AClansReputation";
	
	// NPC
	private static final int VALDIS = 31331;
	
	// Quest Item
	private static final int TYRANNOSAURUS_CLAW = 8767;
	
	public Q510_AClansReputation()
	{
		super(510, "A Clan's Reputation");
		
		setItemsIds(TYRANNOSAURUS_CLAW);
		
		addStartNpc(VALDIS);
		addTalkId(VALDIS);
		
		addKillId(22215, 22216, 22217);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31331-3.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31331-6.htm"))
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
				htmltext = (!player.isClanLeader() || player.getClan().getLevel() < 5) ? "31331-0.htm" : "31331-1.htm";
				break;
			
			case STATE_STARTED:
				final int count = 50 * st.getQuestItemsCount(TYRANNOSAURUS_CLAW);
				if (count > 0)
				{
					final L2Clan clan = player.getClan();
					
					htmltext = "31331-7.htm";
					st.takeItems(TYRANNOSAURUS_CLAW, -1);
					
					clan.addReputationScore(count);
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_QUEST_COMPLETED_AND_S1_POINTS_GAINED).addNumber(count));
					clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				}
				else
					htmltext = "31331-4.htm";
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		// Retrieve the qs of the clan leader.
		QuestState st = getClanLeaderQuestState(player, npc);
		if (st == null || !st.isStarted())
			return null;
		
		st.dropItemsAlways(TYRANNOSAURUS_CLAW, 1, 0);
		
		return null;
	}
}