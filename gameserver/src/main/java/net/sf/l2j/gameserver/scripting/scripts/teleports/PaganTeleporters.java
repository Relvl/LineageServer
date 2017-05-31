/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.scripting.scripts.teleports;

import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class PaganTeleporters extends Quest
{
	// Items
	private static final int VISITOR_MARK = 8064;
	private static final int PAGAN_MARK = 8067;
	
	public PaganTeleporters()
	{
		super(-1, "teleports");
		
		addStartNpc(32034, 32035, 32036, 32037, 32039, 32040);
		addTalkId(32034, 32035, 32036, 32037, 32039, 32040);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("Close_Door1"))
			DoorTable.getInstance().getDoor(19160001).closeMe();
		else if (event.equalsIgnoreCase("Close_Door2"))
		{
			DoorTable.getInstance().getDoor(19160010).closeMe();
			DoorTable.getInstance().getDoor(19160011).closeMe();
		}
		return null;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
			return htmltext;
		
		switch (npc.getNpcId())
		{
			case 32034:
				if (st.hasQuestItems(VISITOR_MARK) || st.hasQuestItems(PAGAN_MARK))
				{
					DoorTable.getInstance().getDoor(19160001).openMe();
					startQuestTimer("Close_Door1", 10000, npc, player, false);
					htmltext = "FadedMark.htm";
				}
				else
				{
					htmltext = "32034-1.htm";
					st.exitQuest(true);
				}
				break;
			
			case 32035:
				DoorTable.getInstance().getDoor(19160001).openMe();
				startQuestTimer("Close_Door1", 10000, npc, player, false);
				htmltext = "FadedMark.htm";
				break;
			
			case 32036:
				if (!st.hasQuestItems(PAGAN_MARK))
					htmltext = "32036-1.htm";
				else
				{
					DoorTable.getInstance().getDoor(19160010).openMe();
					DoorTable.getInstance().getDoor(19160011).openMe();
					startQuestTimer("Close_Door2", 10000, npc, player, false);
					htmltext = "32036-2.htm";
				}
				break;
			
			case 32037:
				DoorTable.getInstance().getDoor(19160010).openMe();
				DoorTable.getInstance().getDoor(19160011).openMe();
				startQuestTimer("Close_Door2", 10000, npc, player, false);
				htmltext = "FadedMark.htm";
				break;
			
			case 32039:
				player.teleToLocation(-12766, -35840, -10856, 0);
				break;
			
			case 32040:
				player.teleToLocation(34962, -49758, -763, 0);
				break;
		}
		return htmltext;
	}
}