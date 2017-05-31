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

import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q123_TheLeaderAndTheFollower extends Quest
{
	private static final String qn = "Q123_TheLeaderAndTheFollower";
	
	// Items
	private static final int BLOOD = 8549;
	private static final int LEG = 8550;
	
	// NPC
	private static final int NEWYEAR = 31961;
	
	// Mobs
	private static final int BRUIN_LIZARDMAN = 27321;
	private static final int PICOT_ARENEID = 27322;
	
	// Rewards
	private static final int CLAN_OATH_HELM = 7850;
	private static final int CLAN_OATH_ARMOR = 7851;
	private static final int CLAN_OATH_GAUNTLETS = 7852;
	private static final int CLAN_OATH_SABATON = 7853;
	private static final int CLAN_OATH_BRIGANDINE = 7854;
	private static final int CLAN_OATH_LEATHER_GLOVES = 7855;
	private static final int CLAN_OATH_BOOTS = 7856;
	private static final int CLAN_OATH_AKETON = 7857;
	private static final int CLAN_OATH_PADDED_GLOVES = 7858;
	private static final int CLAN_OATH_SANDALS = 7859;
	
	public Q123_TheLeaderAndTheFollower()
	{
		super(123, "The Leader and the Follower");
		
		setItemsIds(BLOOD, LEG);
		
		addStartNpc(NEWYEAR);
		addTalkId(NEWYEAR);
		
		addKillId(BRUIN_LIZARDMAN, PICOT_ARENEID);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("31961-02.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31961-05a.htm"))
		{
			if (st.getQuestItemsCount(BLOOD) >= 10)
			{
				st.set("cond", "3");
				st.set("settype", "1");
				st.playSound(QuestState.SOUND_MIDDLE);
				st.takeItems(BLOOD, -1);
			}
			else
				htmltext = "Incorrect item count";
		}
		else if (event.equalsIgnoreCase("31961-05b.htm"))
		{
			if (st.getQuestItemsCount(BLOOD) >= 10)
			{
				st.set("cond", "4");
				st.set("settype", "2");
				st.playSound(QuestState.SOUND_MIDDLE);
				st.takeItems(BLOOD, -1);
			}
			else
				htmltext = "Incorrect item count";
		}
		else if (event.equalsIgnoreCase("31961-05c.htm"))
		{
			if (st.getQuestItemsCount(BLOOD) >= 10)
			{
				st.set("cond", "5");
				st.set("settype", "3");
				st.playSound(QuestState.SOUND_MIDDLE);
				st.takeItems(BLOOD, -1);
			}
			else
				htmltext = "Incorrect item count";
		}
		else if (event.equalsIgnoreCase("31961-09.htm"))
		{
			L2ClanMember cm_apprentice = player.getClan().getClanMember(player.getApprentice());
			if (cm_apprentice.isOnline())
			{
				L2PcInstance apprentice = cm_apprentice.getPlayerInstance();
				if (apprentice != null)
				{
					QuestState apQuest = apprentice.getQuestState(qn);
					if (apQuest != null)
					{
						int crystals = (apQuest.getInt("cond") == 3) ? 922 : 771;
						
						if (st.getQuestItemsCount(1458) >= crystals)
						{
							htmltext = "31961-10.htm";
							
							// Finish sponsor's quest.
							st.takeItems(1458, crystals);
							st.playSound(QuestState.SOUND_FINISH);
							
							// Update academian's quest.
							apQuest.set("cond", "6");
							apQuest.playSound(QuestState.SOUND_MIDDLE);
						}
					}
				}
			}
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
				// Case of academian.
				if (player.getSponsor() > 0)
				{
					if (player.getLevel() > 19 && player.getPledgeType() == -1)
						return "31961-01.htm";
				}
				// Case of sponsor.
				else if (player.getApprentice() > 0)
				{
					L2ClanMember cm = player.getClan().getClanMember(player.getApprentice());
					if (cm != null && cm.isOnline())
					{
						L2PcInstance apprentice = cm.getPlayerInstance();
						if (apprentice != null)
						{
							QuestState apQuest = apprentice.getQuestState(qn);
							if (apQuest != null)
							{
								int apCond = apQuest.getInt("cond");
								if (apCond == 3)
									return "31961-09a.htm";
								else if (apCond == 4)
									return "31961-09b.htm";
								else if (apCond == 5)
									return "31961-09c.htm";
							}
						}
					}
				}
				
				htmltext = "31961-00.htm";
				break;
			
			case STATE_STARTED:
				int cond = st.getInt("cond");
				// Case of academian.
				if (player.getSponsor() > 0)
				{
					if (cond == 1)
						htmltext = "31961-03.htm";
					else if (cond == 2)
						htmltext = "31961-04.htm";
					else if (cond == 3)
						htmltext = "31961-05d.htm";
					else if (cond == 4)
						htmltext = "31961-05e.htm";
					else if (cond == 5)
						htmltext = "31961-05f.htm";
					else if (cond == 6)
					{
						htmltext = "31961-06.htm";
						st.set("cond", "7");
						st.playSound(QuestState.SOUND_MIDDLE);
					}
					else if (cond == 7)
						htmltext = "31961-07.htm";
					else if (cond == 8)
					{
						if (st.getQuestItemsCount(LEG) == 8)
						{
							htmltext = "31961-08.htm";
							
							st.takeItems(LEG, -1);
							st.giveItems(CLAN_OATH_HELM, 1);
							
							switch (st.getInt("settype"))
							{
								case 1:
									st.giveItems(CLAN_OATH_ARMOR, 1);
									st.giveItems(CLAN_OATH_GAUNTLETS, 1);
									st.giveItems(CLAN_OATH_SABATON, 1);
									break;
								
								case 2:
									st.giveItems(CLAN_OATH_BRIGANDINE, 1);
									st.giveItems(CLAN_OATH_LEATHER_GLOVES, 1);
									st.giveItems(CLAN_OATH_BOOTS, 1);
									break;
								
								case 3:
									st.giveItems(CLAN_OATH_AKETON, 1);
									st.giveItems(CLAN_OATH_PADDED_GLOVES, 1);
									st.giveItems(CLAN_OATH_SANDALS, 1);
									break;
							}
							
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
					}
				}
				break;
			
			case STATE_COMPLETED:
				htmltext = getAlreadyCompletedMsg();
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
		
		int sponsor = player.getSponsor();
		if (sponsor == 0)
		{
			st.exitQuest(true);
			return null;
		}
		
		switch (npc.getNpcId())
		{
			case BRUIN_LIZARDMAN:
				if (st.getInt("cond") == 1 && st.dropItems(BLOOD, 1, 10, 600000))
					st.set("cond", "2");
				break;
			
			case PICOT_ARENEID:
				L2ClanMember cmSponsor = player.getClan().getClanMember(sponsor);
				if (cmSponsor != null && cmSponsor.isOnline())
				{
					L2PcInstance sponsorHelper = cmSponsor.getPlayerInstance();
					if (sponsorHelper != null && player.isInsideRadius(sponsorHelper, 1100, true, false))
					{
						if (st.getInt("cond") == 7 && st.dropItems(LEG, 1, 8, 700000))
							st.set("cond", "8");
					}
				}
				break;
		}
		
		return null;
	}
}
