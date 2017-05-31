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

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q021_HiddenTruth extends Quest
{
	private static final String qn = "Q021_HiddenTruth";
	
	// NPCs
	private static final int MYSTERIOUS_WIZARD = 31522;
	private static final int TOMBSTONE = 31523;
	private static final int VON_HELLMAN = 31524;
	private static final int VON_HELLMAN_PAGE = 31525;
	private static final int BROKEN_BOOKSHELF = 31526;
	private static final int AGRIPEL = 31348;
	private static final int DOMINIC = 31350;
	private static final int BENEDICT = 31349;
	private static final int INNOCENTIN = 31328;
	
	// Items
	private static final int CROSS_OF_EINHASAD = 7140;
	private static final int CROSS_OF_EINHASAD_NEXT_QUEST = 7141;
	
	private L2Npc _vonHellmannPage;
	private L2Npc _vonHellmann;
	
	public Q021_HiddenTruth()
	{
		super(21, "Hidden Truth");
		
		setItemsIds(CROSS_OF_EINHASAD);
		
		addStartNpc(MYSTERIOUS_WIZARD);
		addTalkId(MYSTERIOUS_WIZARD, TOMBSTONE, VON_HELLMAN, VON_HELLMAN_PAGE, BROKEN_BOOKSHELF, AGRIPEL, DOMINIC, BENEDICT, INNOCENTIN);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31522-02.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31523-03.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
			spawnVonHellmann(st);
		}
		else if (event.equalsIgnoreCase("31524-06.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
			
			// Spawn the page.
			if (_vonHellmannPage == null)
			{
				_vonHellmannPage = addSpawn(VON_HELLMAN_PAGE, 51462, -54539, -3176, 0, false, 90000, true);
				_vonHellmannPage.broadcastNpcSay("My master has instructed me to be your guide, " + player.getName() + ".");
				
				// Make it move.
				startQuestTimer("1", 4000, _vonHellmannPage, player, false);
				startQuestTimer("pageDespawn", 88000, _vonHellmannPage, player, false);
			}
		}
		else if (event.equalsIgnoreCase("31526-08.htm"))
		{
			st.set("cond", "5");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31526-14.htm"))
		{
			st.set("cond", "6");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.giveItems(CROSS_OF_EINHASAD, 1);
		}
		else if (event.equalsIgnoreCase("1"))
		{
			_vonHellmannPage.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(52373, -54296, -3136, 0));
			_vonHellmannPage.broadcastNpcSay("Follow me...");
			startQuestTimer("2", 5000, _vonHellmannPage, player, false);
			return null;
		}
		else if (event.equalsIgnoreCase("2"))
		{
			_vonHellmannPage.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(52279, -53064, -3161, 0));
			startQuestTimer("3", 12000, _vonHellmannPage, player, false);
			return null;
		}
		else if (event.equalsIgnoreCase("3"))
		{
			_vonHellmannPage.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(51909, -51725, -3125, 0));
			startQuestTimer("4", 15000, _vonHellmannPage, player, false);
			return null;
		}
		else if (event.equalsIgnoreCase("4"))
		{
			_vonHellmannPage.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(52438, -51240, -3097, 0));
			_vonHellmannPage.broadcastNpcSay("This where that here...");
			startQuestTimer("5", 5000, _vonHellmannPage, player, false);
			return null;
		}
		else if (event.equalsIgnoreCase("5"))
		{
			_vonHellmannPage.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(52143, -51418, -3085, 0));
			_vonHellmannPage.broadcastNpcSay("I want to speak to you...");
			return null;
		}
		else if (event.equalsIgnoreCase("31328-05.htm"))
		{
			if (st.hasQuestItems(CROSS_OF_EINHASAD))
			{
				st.takeItems(CROSS_OF_EINHASAD, 1);
				st.giveItems(CROSS_OF_EINHASAD_NEXT_QUEST, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
		}
		else if (event.equalsIgnoreCase("pageDespawn"))
			_vonHellmannPage = null;
		
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
				htmltext = (player.getLevel() < 63) ? "31522-03.htm" : "31522-01.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case MYSTERIOUS_WIZARD:
						htmltext = "31522-05.htm";
						break;
					
					case TOMBSTONE:
						if (cond == 1)
							htmltext = "31523-01.htm";
						else if (cond == 2 || cond == 3)
						{
							htmltext = "31523-04.htm";
							spawnVonHellmann(st);
						}
						else if (cond > 3)
							htmltext = "31523-04.htm";
						break;
					
					case VON_HELLMAN:
						if (cond == 2)
							htmltext = "31524-01.htm";
						else if (cond == 3)
							htmltext = "31524-07.htm";
						else if (cond > 3)
							htmltext = "31524-07a.htm";
						break;
					
					case VON_HELLMAN_PAGE:
						if (cond == 3 || cond == 4)
						{
							htmltext = "31525-01.htm";
							if (!_vonHellmannPage.isMoving())
							{
								htmltext = "31525-02.htm";
								if (cond == 3)
								{
									st.set("cond", "4");
									st.playSound(QuestState.SOUND_MIDDLE);
								}
							}
						}
						break;
					
					case BROKEN_BOOKSHELF:
						if (cond == 3 || cond == 4)
						{
							htmltext = "31526-01.htm";
							
							if (!_vonHellmannPage.isMoving())
							{
								st.set("cond", "5");
								st.playSound(QuestState.SOUND_MIDDLE);
								
								if (_vonHellmannPage != null)
								{
									_vonHellmannPage.deleteMe();
									_vonHellmannPage = null;
									
									cancelQuestTimer("pageDespawn", null, player);
								}
								
								if (_vonHellmann != null)
								{
									_vonHellmann.deleteMe();
									_vonHellmann = null;
								}
							}
						}
						else if (cond == 5)
							htmltext = "31526-10.htm";
						else if (cond > 5)
							htmltext = "31526-15.htm";
						break;
					
					case AGRIPEL:
					case BENEDICT:
					case DOMINIC:
						if ((cond == 6 || cond == 7) && st.hasQuestItems(CROSS_OF_EINHASAD))
						{
							int npcId = npc.getNpcId();
							
							// For cond 6, make checks until cond 7 is activated.
							if (cond == 6)
							{
								int npcId1 = 0, npcId2 = 0;
								if (npcId == AGRIPEL)
								{
									npcId1 = BENEDICT;
									npcId2 = DOMINIC;
								}
								else if (npcId == BENEDICT)
								{
									npcId1 = AGRIPEL;
									npcId2 = DOMINIC;
								}
								else if (npcId == DOMINIC)
								{
									npcId1 = AGRIPEL;
									npcId2 = BENEDICT;
								}
								
								if (st.getInt(String.valueOf(npcId1)) == 1 && st.getInt(String.valueOf(npcId2)) == 1)
								{
									st.set("cond", "7");
									st.playSound(QuestState.SOUND_MIDDLE);
								}
								else
									st.set(String.valueOf(npcId), "1");
							}
							
							htmltext = npcId + "-01.htm";
						}
						break;
					
					case INNOCENTIN:
						if (cond == 7 && st.hasQuestItems(CROSS_OF_EINHASAD))
							htmltext = "31328-01.htm";
						break;
				}
				break;
			
			case STATE_COMPLETED:
				if (npc.getNpcId() == INNOCENTIN)
					htmltext = "31328-06.htm";
				else
					htmltext = getAlreadyCompletedMsg();
				break;
		}
		
		return htmltext;
	}
	
	private void spawnVonHellmann(QuestState st)
	{
		if (_vonHellmann == null)
		{
			_vonHellmann = addSpawn(VON_HELLMAN, 51432, -54570, -3136, 0, false, 0, true);
			_vonHellmann.broadcastNpcSay("Who awoke me?");
		}
	}
}