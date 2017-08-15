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
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q271_ProofOfValor extends Quest
{
	private static final String qn = "Q271_ProofOfValor";
	
	// Item
	private static final int KASHA_WOLF_FANG = 1473;
	
	// Rewards
	private static final int NECKLACE_OF_VALOR = 1507;
	private static final int NECKLACE_OF_COURAGE = 1506;
	
	public Q271_ProofOfValor()
	{
		super(271, "Proof of Valor");
		
		setItemsIds(KASHA_WOLF_FANG);
		
		addStartNpc(30577); // Rukain
		addTalkId(30577);
		
		addKillId(20475); // Kasha Wolf
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30577-03.htm"))
		{
			st.setState(QuestState.STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			
			if (st.hasAtLeastOneQuestItem(NECKLACE_OF_COURAGE, NECKLACE_OF_VALOR))
				htmltext = "30577-07.htm";
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
			case QuestState.STATE_CREATED:
				if (player.getRace() != PlayerRace.Orc)
					htmltext = "30577-00.htm";
				else if (player.getLevel() < 4)
					htmltext = "30577-01.htm";
				else
					htmltext = (st.hasAtLeastOneQuestItem(NECKLACE_OF_COURAGE, NECKLACE_OF_VALOR)) ? "30577-06.htm" : "30577-02.htm";
				break;
			
			case QuestState.STATE_STARTED:
				if (st.getInt("cond") == 1)
					htmltext = (st.hasAtLeastOneQuestItem(NECKLACE_OF_COURAGE, NECKLACE_OF_VALOR)) ? "30577-07.htm" : "30577-04.htm";
				else
				{
					htmltext = "30577-05.htm";
					st.takeItems(KASHA_WOLF_FANG, -1);
					st.giveItems((Rnd.get(100) < 10) ? NECKLACE_OF_VALOR : NECKLACE_OF_COURAGE, 1);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = checkPlayerCondition(player, npc, "cond", "1");
		if (st == null)
			return null;
		
		if (st.dropItemsAlways(KASHA_WOLF_FANG, (Rnd.get(4) == 0) ? 2 : 1, 50))
			st.set("cond", "2");
		
		return null;
	}
}