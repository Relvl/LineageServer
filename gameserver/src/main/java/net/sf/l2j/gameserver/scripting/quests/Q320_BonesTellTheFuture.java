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

public class Q320_BonesTellTheFuture extends Quest
{
	private static final String qn = "Q320_BonesTellTheFuture";
	
	// Quest item
	private final int BONE_FRAGMENT = 809;
	
	public Q320_BonesTellTheFuture()
	{
		super(320, "Bones Tell the Future");
		
		setItemsIds(BONE_FRAGMENT);
		
		addStartNpc(30359); // Kaitar
		addTalkId(30359);
		
		addKillId(20517, 20518);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30359-04.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		
		return event;
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
				if (player.getRace() != PlayerRace.DarkElf)
					htmltext = "30359-00.htm";
				else if (player.getLevel() < 10)
					htmltext = "30359-02.htm";
				else
					htmltext = "30359-03.htm";
				break;
			
			case STATE_STARTED:
				if (st.getInt("cond") == 1)
					htmltext = "30359-05.htm";
				else
				{
					htmltext = "30359-06.htm";
					st.takeItems(BONE_FRAGMENT, -1);
					st.rewardItems(ItemConst.ADENA_ID, 8470);
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
		
		if (st.dropItems(BONE_FRAGMENT, 1, 10, (npc.getNpcId() == 20517) ? 180000 : 200000))
			st.set("cond", "2");
		
		return null;
	}
}