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
import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q316_DestroyPlagueCarriers extends Quest
{
	private static final String qn = "Q316_DestroyPlagueCarriers";
	
	// Items
	private static final int WERERAT_FANG = 1042;
	private static final int VAROOL_FOULCLAW_FANG = 1043;
	
	// Monsters
	private static final int SUKAR_WERERAT = 20040;
	private static final int SUKAR_WERERAT_LEADER = 20047;
	private static final int VAROOL_FOULCLAW = 27020;
	
	public Q316_DestroyPlagueCarriers()
	{
		super(316, "Destroy Plague Carriers");
		
		setItemsIds(WERERAT_FANG, VAROOL_FOULCLAW_FANG);
		
		addStartNpc(30155); // Ellenia
		addTalkId(30155);
		
		addKillId(SUKAR_WERERAT, SUKAR_WERERAT_LEADER, VAROOL_FOULCLAW);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30155-04.htm"))
		{
			st.setState(QuestState.STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30155-08.htm"))
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
			case QuestState.STATE_CREATED:
				if (player.getRace() != PlayerRace.Elf)
					htmltext = "30155-00.htm";
				else if (player.getLevel() < 18)
					htmltext = "30155-02.htm";
				else
					htmltext = "30155-03.htm";
				break;
			
			case QuestState.STATE_STARTED:
				final int ratFangs = st.getQuestItemsCount(WERERAT_FANG);
				final int varoolFangs = st.getQuestItemsCount(VAROOL_FOULCLAW_FANG);
				
				if (ratFangs + varoolFangs == 0)
					htmltext = "30155-05.htm";
				else
				{
					htmltext = "30155-07.htm";
					st.takeItems(WERERAT_FANG, -1);
					st.takeItems(VAROOL_FOULCLAW_FANG, -1);
					st.rewardItems(ItemConst.ADENA_ID, ratFangs * 30 + varoolFangs * 10000 + ((ratFangs > 10) ? 5000 : 0));
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = checkPlayerState(player, npc, QuestState.STATE_STARTED);
		if (st == null)
			return null;
		
		switch (npc.getNpcId())
		{
			case SUKAR_WERERAT:
			case SUKAR_WERERAT_LEADER:
				st.dropItems(WERERAT_FANG, 1, 0, 400000);
				break;
			
			case VAROOL_FOULCLAW:
				st.dropItems(VAROOL_FOULCLAW_FANG, 1, 1, 200000);
				break;
		}
		
		return null;
	}
}