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
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

/**
 * iCond is an internal variable, used because cond isn't developped on that quest (only 3 states) :
 * <ul>
 * <li>1-3 leads initial mimyu behavior ;</li>
 * <li>used for leaves support as mask : 4, 8, 16 or 32 = 60 overall ;</li>
 * <li>63 becomes the "marker" to get back to mimyu (60 + 3), meaning you hitted the 4 trees ;</li>
 * <li>setted to 100 if mimyu check is ok.</li>
 * </ul>
 */
public class Q421_LittleWingsBigAdventure extends Quest
{
	private static final String qn = "Q421_LittleWingsBigAdventure";
	
	// NPCs
	private static final int CRONOS = 30610;
	private static final int MIMYU = 30747;
	
	// Item
	private static final int FAIRY_LEAF = 4325;
	
	public Q421_LittleWingsBigAdventure()
	{
		super(421, "Little Wing's Big Adventure");
		
		setItemsIds(FAIRY_LEAF);
		
		addStartNpc(CRONOS);
		addTalkId(CRONOS, MIMYU);
		
		addAttackId(27185, 27186, 27187, 27188);
		addKillId(27185, 27186, 27187, 27188);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		if (event.equalsIgnoreCase("30610-06.htm"))
		{
			if (st.getQuestItemsCount(3500) + st.getQuestItemsCount(3501) + st.getQuestItemsCount(3502) == 1)
			{
				// Find the level of the flute.
				for (int i = 3500; i < 3503; i++)
				{
					final L2ItemInstance item = player.getInventory().getItemByItemId(i);
					if (item != null && item.getEnchantLevel() >= 55)
					{
						st.setState(STATE_STARTED);
						st.set("cond", "1");
						st.set("iCond", "1");
						st.set("summonOid", String.valueOf(item.getObjectId()));
						st.playSound(QuestState.SOUND_ACCEPT);
						return "30610-05.htm";
					}
				}
			}
			
			// Exit quest if you got more than one flute, or the flute level doesn't meat requirements.
			st.exitQuest(true);
		}
		else if (event.equalsIgnoreCase("30747-02.htm"))
		{
			final L2Summon summon = player.getPet();
			if (summon != null)
				htmltext = (summon.getControlItemId() == st.getInt("summonOid")) ? "30747-04.htm" : "30747-03.htm";
		}
		else if (event.equalsIgnoreCase("30747-05.htm"))
		{
			final L2Summon summon = player.getPet();
			if (summon == null || summon.getControlItemId() != st.getInt("summonOid"))
				htmltext = "30747-06.htm";
			else
			{
				st.set("cond", "2");
				st.set("iCond", "3");
				st.playSound(QuestState.SOUND_MIDDLE);
				st.giveItems(FAIRY_LEAF, 4);
			}
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
				// Wrong level.
				if (player.getLevel() < 45)
					htmltext = "30610-01.htm";
				// Got more than one flute, or none.
				else if (st.getQuestItemsCount(3500) + st.getQuestItemsCount(3501) + st.getQuestItemsCount(3502) != 1)
					htmltext = "30610-02.htm";
				else
				{
					// Find the level of the hatchling.
					for (int i = 3500; i < 3503; i++)
					{
						final L2ItemInstance item = player.getInventory().getItemByItemId(i);
						if (item != null && item.getEnchantLevel() >= 55)
							return "30610-04.htm";
					}
					
					// Invalid level.
					htmltext = "30610-03.htm";
				}
				break;
			
			case STATE_STARTED:
				switch (npc.getNpcId())
				{
					case CRONOS:
						htmltext = "30610-07.htm";
						break;
					
					case MIMYU:
						final int id = st.getInt("iCond");
						if (id == 1)
						{
							htmltext = "30747-01.htm";
							st.set("iCond", "2");
						}
						else if (id == 2)
						{
							final L2Summon summon = player.getPet();
							htmltext = (summon != null) ? ((summon.getControlItemId() == st.getInt("summonOid")) ? "30747-04.htm" : "30747-03.htm") : "30747-02.htm";
						}
						else if (id == 3) // Explanation is done, leaves are already given.
							htmltext = "30747-07.htm";
						else if (id > 3 && id < 63) // Did at least one tree, but didn't manage to make them all.
							htmltext = "30747-11.htm";
						else if (id == 63) // Did all trees, no more leaves.
						{
							final L2Summon summon = player.getPet();
							if (summon == null)
								return "30747-12.htm";
							
							if (summon.getControlItemId() != st.getInt("summonOid"))
								return "30747-14.htm";
							
							htmltext = "30747-13.htm";
							st.set("iCond", "100");
						}
						else if (id == 100) // Spoke with the Fairy.
						{
							final L2Summon summon = player.getPet();
							if (summon != null && summon.getControlItemId() == st.getInt("summonOid"))
								return "30747-15.htm";
							
							if (st.getQuestItemsCount(3500) + st.getQuestItemsCount(3501) + st.getQuestItemsCount(3502) > 1)
								return "30747-17.htm";
							
							for (int i = 3500; i < 3503; i++)
							{
								final L2ItemInstance item = player.getInventory().getItemByItemId(i);
								if (item != null && item.getObjectId() == st.getInt("summonOid"))
								{
									st.takeItems(i, 1);
									st.giveItems(i + 922, 1, item.getEnchantLevel()); // TODO rebuild entirely pet system in order enchant is given a fuck. Supposed to give an item lvl XX for a flute level XX.
									st.playSound(QuestState.SOUND_FINISH);
									st.exitQuest(true);
									return "30747-16.htm";
								}
							}
							
							// Curse if the registered objectId is the wrong one (switch flutes).
							htmltext = "30747-18.htm";
							
							final L2Skill skill = SkillTable.getInstance().getInfo(4167, 1);
							if (skill != null && player.getFirstEffect(skill) == null)
								skill.getEffects(npc, player);
						}
						break;
				}
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		// Minions scream no matter current quest state.
		if (((L2MonsterInstance) npc).hasMinions())
		{
			for (L2MonsterInstance ghost : ((L2MonsterInstance) npc).getMinionList().getSpawnedMinions())
			{
				if (!ghost.isDead() && Rnd.get(100) < 1)
					ghost.broadcastNpcSay("We must protect the fairy tree!");
			}
		}
		
		// Condition required : 2.
		QuestState st = checkPlayerCondition(attacker, npc, "cond", "2");
		if (st == null)
			return null;
		
		// A pet was the attacker, and the objectId is the good one.
		if (isPet && attacker.getPet().getControlItemId() == st.getInt("summonOid"))
		{
			// Random luck is reached and you still have some leaves ; go further.
			if (Rnd.get(100) < 1 && st.hasQuestItems(FAIRY_LEAF))
			{
				final int idMask = (int) Math.pow(2, (npc.getNpcId() - 27182) - 1);
				final int iCond = st.getInt("iCond");
				
				if ((iCond | idMask) != iCond)
				{
					st.set("iCond", String.valueOf(iCond | idMask));
					
					npc.broadcastNpcSay("Give me a Fairy Leaf...!");
					st.takeItems(FAIRY_LEAF, 1);
					npc.broadcastNpcSay("Leave now, before you incur the wrath of the guardian ghost...");
					
					// Four leafs have been used ; update quest state.
					if (st.getInt("iCond") == 63)
					{
						st.set("cond", "3");
						st.playSound(QuestState.SOUND_MIDDLE);
					}
					else
						st.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		final L2Character originalKiller = isPet ? killer.getPet() : killer;
		
		// Tree curses the killer.
		if (Rnd.get(100) < 30)
		{
			if (originalKiller != null)
			{
				final L2Skill skill = SkillTable.getInstance().getInfo(4243, 1);
				if (skill != null && originalKiller.getFirstEffect(skill) == null)
					skill.getEffects(npc, originalKiller);
			}
		}
		
		// Spawn 20 ghosts, attacking the killer.
		for (int i = 0; i < 20; i++)
		{
			final L2Attackable newNpc = (L2Attackable) addSpawn(27189, npc, true, 300000, false);
			
			newNpc.setRunning();
			newNpc.addDamageHate(originalKiller, 0, 999);
			newNpc.getAI().setIntention(EIntention.ATTACK, originalKiller);
		}
		
		return super.onKill(npc, killer, isPet);
	}
}