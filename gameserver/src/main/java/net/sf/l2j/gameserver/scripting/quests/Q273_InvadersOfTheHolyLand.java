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
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q273_InvadersOfTheHolyLand extends Quest
{
	private static final String qn = "Q273_InvadersOfTheHolyLand";
	
	// Items
	private static final int BLACK_SOULSTONE = 1475;
	private static final int RED_SOULSTONE = 1476;
	
	// Reward
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	
	public Q273_InvadersOfTheHolyLand()
	{
		super(273, "Invaders of the Holy Land");
		
		setItemsIds(BLACK_SOULSTONE, RED_SOULSTONE);
		
		addStartNpc(30566); // Varkees
		addTalkId(30566);
		
		addKillId(20311, 20312, 20313);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30566-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30566-07.htm"))
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
				if (player.getRace() != PlayerRace.Orc)
					htmltext = "30566-00.htm";
				else if (player.getLevel() < 6)
					htmltext = "30566-01.htm";
				else
					htmltext = "30566-02.htm";
				break;
			
			case STATE_STARTED:
				int red = st.getQuestItemsCount(RED_SOULSTONE);
				int black = st.getQuestItemsCount(BLACK_SOULSTONE);
				
				if (red + black == 0)
					htmltext = "30566-04.htm";
				else
				{
					if (red == 0)
						htmltext = "30566-05.htm";
					else
						htmltext = "30566-06.htm";
					
					int reward = (black * 3) + (red * 10) + ((black >= 10) ? ((red >= 1) ? 1800 : 1500) : 0);
					
					st.takeItems(BLACK_SOULSTONE, -1);
					st.takeItems(RED_SOULSTONE, -1);
					st.rewardItems(ItemConst.ADENA_ID, reward);
					
					if (player.isNewbie() && st.getInt("Reward") == 0)
					{
						st.giveItems(SOULSHOT_FOR_BEGINNERS, 6000);
						st.playTutorialVoice("tutorial_voice_026");
						st.set("Reward", "1");
					}
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
		
		final int npcId = npc.getNpcId();
		
		int probability = 77;
		if (npcId == 20311)
			probability = 90;
		else if (npcId == 20312)
			probability = 87;
		
		if (Rnd.get(100) <= probability)
			st.dropItemsAlways(BLACK_SOULSTONE, 1, 0);
		else
			st.dropItemsAlways(RED_SOULSTONE, 1, 0);
		
		return null;
	}
}