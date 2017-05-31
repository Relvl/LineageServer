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
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.SpawnLocation;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q652_AnAgedExAdventurer extends Quest
{
	private static final String qn = "Q652_AnAgedExAdventurer";
	
	// NPCs
	private static final int TANTAN = 32012;
	private static final int SARA = 30180;
	
	// Item
	private static final int SOULSHOT_C = 1464;
	
	// Reward
	private static final int ENCHANT_ARMOR_D = 956;
	
	// Table of possible spawns
	private static final SpawnLocation[] SPAWNS =
	{
		new SpawnLocation(78355, -1325, -3659, 0),
		new SpawnLocation(79890, -6132, -2922, 0),
		new SpawnLocation(90012, -7217, -3085, 0),
		new SpawnLocation(94500, -10129, -3290, 0),
		new SpawnLocation(96534, -1237, -3677, 0)
	};
	
	// Current position
	private int _currentPosition = 0;
	
	public Q652_AnAgedExAdventurer()
	{
		super(652, "An Aged Ex-Adventurer");
		
		addStartNpc(TANTAN);
		addTalkId(TANTAN, SARA);
		
		addSpawn(TANTAN, 78355, -1325, -3659, 0, false, 0, false);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("32012-02.htm"))
		{
			if (st.getQuestItemsCount(SOULSHOT_C) >= 100)
			{
				st.setState(STATE_STARTED);
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
				st.takeItems(SOULSHOT_C, 100);
				
				npc.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(85326, 7869, -3620, 0));
				startQuestTimer("apparition_npc", 6000, npc, player, false);
			}
			else
			{
				htmltext = "32012-02a.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equalsIgnoreCase("apparition_npc"))
		{
			int chance = Rnd.get(5);
			
			// Loop to avoid to spawn to the same place.
			while (chance == _currentPosition)
				chance = Rnd.get(5);
			
			// Register new position.
			_currentPosition = chance;
			
			npc.deleteMe();
			addSpawn(TANTAN, SPAWNS[chance], false, 0, false);
			return null;
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
				htmltext = (player.getLevel() < 46) ? "32012-00.htm" : "32012-01.htm";
				break;
			
			case STATE_STARTED:
				switch (npc.getNpcId())
				{
					case SARA:
						if (Rnd.get(100) < 50)
						{
							htmltext = "30180-01.htm";
							st.rewardItems(57, 5026);
							st.giveItems(ENCHANT_ARMOR_D, 1);
						}
						else
						{
							htmltext = "30180-02.htm";
							st.rewardItems(57, 10000);
						}
						st.playSound(QuestState.SOUND_FINISH);
						st.exitQuest(true);
						break;
					
					case TANTAN:
						htmltext = "32012-04a.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
}