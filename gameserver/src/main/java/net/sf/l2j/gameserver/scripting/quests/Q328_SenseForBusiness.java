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

public class Q328_SenseForBusiness extends Quest
{
	private static final String qn = "Q328_SenseForBusiness";
	
	// Items
	private static final int MONSTER_EYE_LENS = 1366;
	private static final int MONSTER_EYE_CARCASS = 1347;
	private static final int BASILISK_GIZZARD = 1348;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	{
		CHANCES.put(20055, 48);
		CHANCES.put(20059, 52);
		CHANCES.put(20067, 68);
		CHANCES.put(20068, 76);
		CHANCES.put(20070, 500000);
		CHANCES.put(20072, 510000);
	}
	
	public Q328_SenseForBusiness()
	{
		super(328, "Sense for Business");
		
		setItemsIds(MONSTER_EYE_LENS, MONSTER_EYE_CARCASS, BASILISK_GIZZARD);
		
		addStartNpc(30436); // Sarien
		addTalkId(30436);
		
		addKillId(20055, 20059, 20067, 20068, 20070, 20072);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30436-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30436-06.htm"))
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
				htmltext = (player.getLevel() < 21) ? "30436-01.htm" : "30436-02.htm";
				break;
			
			case STATE_STARTED:
				final int carcasses = st.getQuestItemsCount(MONSTER_EYE_CARCASS);
				final int lenses = st.getQuestItemsCount(MONSTER_EYE_LENS);
				final int gizzards = st.getQuestItemsCount(BASILISK_GIZZARD);
				
				final int all = carcasses + lenses + gizzards;
				
				if (all == 0)
					htmltext = "30436-04.htm";
				else
				{
					htmltext = "30436-05.htm";
					st.takeItems(MONSTER_EYE_CARCASS, -1);
					st.takeItems(MONSTER_EYE_LENS, -1);
					st.takeItems(BASILISK_GIZZARD, -1);
					st.rewardItems(57, (25 * carcasses) + (1000 * lenses) + (60 * gizzards) + ((all >= 10) ? 618 : 0));
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
		final int chance = CHANCES.get(npcId);
		
		if (npcId < 20069)
		{
			final int rnd = Rnd.get(100);
			if (rnd < (chance + 1))
				st.dropItemsAlways((rnd < chance) ? MONSTER_EYE_CARCASS : MONSTER_EYE_LENS, 1, 0);
		}
		else
			st.dropItems(BASILISK_GIZZARD, 1, 0, chance);
		
		return null;
	}
}