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

import java.util.logging.Level;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager.StatusEnum;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q625_TheFinestIngredients_Part2 extends Quest
{
	private static final String qn = "Q625_TheFinestIngredients_Part2";
	
	// Monster
	private static final int ICICLE_EMPEROR_BUMBALUMP = 25296;
	
	// NPCs
	private static final int JEREMY = 31521;
	private static final int YETI_TABLE = 31542;
	
	// Items
	private static final int SOY_SAUCE_JAR = 7205;
	private static final int FOOD_FOR_BUMBALUMP = 7209;
	private static final int SPECIAL_YETI_MEAT = 7210;
	private static final int REWARD_DYE[] =
	{
		4589,
		4590,
		4591,
		4592,
		4593,
		4594
	};
	
	// Other
	private static final int CHECK_INTERVAL = 600000; // 10 minutes
	private static final int IDLE_INTERVAL = 3; // (X * CHECK_INTERVAL) = 30 minutes
	private static L2Npc _npc = null;
	private static int _status = -1;
	
	public Q625_TheFinestIngredients_Part2()
	{
		super(625, "The Finest Ingredients - Part 2");
		
		setItemsIds(FOOD_FOR_BUMBALUMP, SPECIAL_YETI_MEAT);
		
		addStartNpc(JEREMY);
		addTalkId(JEREMY, YETI_TABLE);
		
		addAttackId(ICICLE_EMPEROR_BUMBALUMP);
		addKillId(ICICLE_EMPEROR_BUMBALUMP);
		
		switch (RaidBossSpawnManager.getInstance().getRaidBossStatusId(ICICLE_EMPEROR_BUMBALUMP))
		{
			case UNDEFINED:
				_log.log(Level.WARNING, qn + ": can not find spawned L2RaidBoss id=" + ICICLE_EMPEROR_BUMBALUMP);
				break;
			
			case ALIVE:
				spawnNpc();
			case DEAD:
				startQuestTimer("check", CHECK_INTERVAL, null, null, true);
				break;
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		// global quest timer has player==null -> cannot get QuestState
		if (event.equals("check"))
		{
			L2RaidBossInstance raid = RaidBossSpawnManager.getInstance().getBosses().get(ICICLE_EMPEROR_BUMBALUMP);
			if (raid != null && raid.getRaidStatus() == StatusEnum.ALIVE)
			{
				if (_status >= 0 && _status-- == 0)
					despawnRaid(raid);
				
				spawnNpc();
			}
			
			return null;
		}
		
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		// Jeremy
		if (event.equalsIgnoreCase("31521-03.htm"))
		{
			if (st.hasQuestItems(SOY_SAUCE_JAR))
			{
				st.setState(STATE_STARTED);
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
				st.takeItems(SOY_SAUCE_JAR, 1);
				st.giveItems(FOOD_FOR_BUMBALUMP, 1);
			}
			else
				htmltext = "31521-04.htm";
		}
		else if (event.equalsIgnoreCase("31521-08.htm"))
		{
			if (st.hasQuestItems(SPECIAL_YETI_MEAT))
			{
				st.takeItems(SPECIAL_YETI_MEAT, 1);
				st.rewardItems(REWARD_DYE[Rnd.get(REWARD_DYE.length)], 5);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
			else
				htmltext = "31521-09.htm";
		}
		// Yeti's Table
		else if (event.equalsIgnoreCase("31542-02.htm"))
		{
			if (st.hasQuestItems(FOOD_FOR_BUMBALUMP))
			{
				if (_status < 0)
				{
					if (spawnRaid())
					{
						st.set("cond", "2");
						st.playSound(QuestState.SOUND_MIDDLE);
						st.takeItems(FOOD_FOR_BUMBALUMP, 1);
					}
				}
				else
					htmltext = "31542-04.htm";
			}
			else
				htmltext = "31542-03.htm";
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
				htmltext = (player.getLevel() < 73) ? "31521-02.htm" : "31521-01.htm";
				break;
			
			case STATE_STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case JEREMY:
						if (cond == 1)
							htmltext = "31521-05.htm";
						else if (cond == 2)
							htmltext = "31521-06.htm";
						else
							htmltext = "31521-07.htm";
						break;
					
					case YETI_TABLE:
						if (cond == 1)
							htmltext = "31542-01.htm";
						else if (cond == 2)
							htmltext = "31542-05.htm";
						break;
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		_status = IDLE_INTERVAL;
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		for (L2PcInstance partyMember : getPartyMembers(player, npc, "cond", "2"))
		{
			QuestState st = partyMember.getQuestState(qn);
			
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.giveItems(SPECIAL_YETI_MEAT, 1);
		}
		
		// despawn raid (reset info)
		despawnRaid(npc);
		
		// despawn npc
		if (_npc != null)
		{
			_npc.deleteMe();
			_npc = null;
		}
		
		return null;
	}
	
	private void spawnNpc()
	{
		// spawn npc, if not spawned
		if (_npc == null)
			_npc = addSpawn(YETI_TABLE, 157136, -121456, -2363, 40000, false, 0, false);
	}
	
	private static boolean spawnRaid()
	{
		L2RaidBossInstance raid = RaidBossSpawnManager.getInstance().getBosses().get(ICICLE_EMPEROR_BUMBALUMP);
		if (raid.getRaidStatus() == StatusEnum.ALIVE)
		{
			// set temporarily spawn location (to provide correct behavior of L2RaidBossInstance.checkAndReturnToSpawn())
			raid.getSpawn().setLocx(157117);
			raid.getSpawn().setLocy(-121939);
			raid.getSpawn().setLocz(-2397);
			
			// teleport raid from secret place
			raid.teleToLocation(157117, -121939, -2397, 100);
			raid.broadcastNpcSay("Hmmm, what do I smell over here?"); // FIXME missing retail message
			
			// set raid status
			_status = IDLE_INTERVAL;
			
			return true;
		}
		
		return false;
	}
	
	private static void despawnRaid(L2Npc raid)
	{
		// reset spawn location
		raid.getSpawn().setLocx(-104700);
		raid.getSpawn().setLocy(-252700);
		raid.getSpawn().setLocz(-15542);
		
		// teleport raid back to secret place
		if (!raid.isDead())
			raid.teleToLocation(-104700, -252700, -15542, 0);
		
		// reset raid status
		_status = -1;
	}
}