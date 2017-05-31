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
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q382_KailsMagicCoin extends Quest
{
	private static final String qn = "Q382_KailsMagicCoin";
	
	// Monsters
	private static final int FALLEN_ORC = 21017;
	private static final int FALLEN_ORC_ARCHER = 21019;
	private static final int FALLEN_ORC_SHAMAN = 21020;
	private static final int FALLEN_ORC_CAPTAIN = 21022;
	
	// Items
	private static final int ROYAL_MEMBERSHIP = 5898;
	private static final int SILVER_BASILISK = 5961;
	private static final int GOLD_GOLEM = 5962;
	private static final int BLOOD_DRAGON = 5963;
	
	public Q382_KailsMagicCoin()
	{
		super(382, "Kail's Magic Coin");
		
		setItemsIds(SILVER_BASILISK, GOLD_GOLEM, BLOOD_DRAGON);
		
		addStartNpc(30687); // Vergara
		addTalkId(30687);
		
		addKillId(FALLEN_ORC, FALLEN_ORC_ARCHER, FALLEN_ORC_SHAMAN, FALLEN_ORC_CAPTAIN);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30687-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
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
				htmltext = (player.getLevel() < 55 || !st.hasQuestItems(ROYAL_MEMBERSHIP)) ? "30687-01.htm" : "30687-02.htm";
				break;
			
			case STATE_STARTED:
				htmltext = "30687-04.htm";
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
		
		switch (npc.getNpcId())
		{
			case FALLEN_ORC:
				st.dropItems(SILVER_BASILISK, 1, 0, 100000);
				break;
			
			case FALLEN_ORC_ARCHER:
				st.dropItems(GOLD_GOLEM, 1, 0, 100000);
				break;
			
			case FALLEN_ORC_SHAMAN:
				st.dropItems(BLOOD_DRAGON, 1, 0, 100000);
				break;
			
			case FALLEN_ORC_CAPTAIN:
				st.dropItems(5961 + Rnd.get(3), 1, 0, 100000);
				break;
		}
		
		return null;
	}
}