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
package net.sf.l2j.gameserver.model.actor.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.HerbDropTable;
import net.sf.l2j.gameserver.model.L2MinionData;
import net.sf.l2j.gameserver.model.L2NpcAIData;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2XmassTreeInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * This class contains the generic data of a L2Spawn object.
 */
public final class NpcTemplate extends CharTemplate
{
	protected static final Logger _log = Logger.getLogger(NpcTemplate.class.getName());
	
	private final int _npcId;
	private final int _idTemplate;
	private final String _type;
	private final String _name;
	private final String _title;
	private final byte _level;
	private final int _exp;
	private final int _sp;
	private final int _rHand;
	private final int _lHand;
	private final int _enchantEffect;
	private final int _corpseTime;
	private int _dropHerbGroup;
	private Race _race;
	
	// used for champion option ; avoid to popup champion quest mob.
	private final boolean _cantBeChampionMonster;
	
	// Skills arrays
	private final List<L2Skill> _buffSkills = new ArrayList<>();
	private final List<L2Skill> _debuffSkills = new ArrayList<>();
	private final List<L2Skill> _healSkills = new ArrayList<>();
	private final List<L2Skill> _longRangeSkills = new ArrayList<>();
	private final List<L2Skill> _shortRangeSkills = new ArrayList<>();
	private final List<L2Skill> _suicideSkills = new ArrayList<>();
	
	private L2NpcAIData _AIdata = new L2NpcAIData();
	
	public static enum AIType
	{
		DEFAULT,
		ARCHER,
		MAGE,
		HEALER,
		CORPSE
	}
	
	public static enum Race
	{
		UNDEAD,
		MAGICCREATURE,
		BEAST,
		ANIMAL,
		PLANT,
		HUMANOID,
		SPIRIT,
		ANGEL,
		DEMON,
		DRAGON,
		GIANT,
		BUG,
		FAIRIE,
		HUMAN,
		ELVE,
		DARKELVE,
		ORC,
		DWARVE,
		OTHER,
		NONLIVING,
		SIEGEWEAPON,
		DEFENDINGARMY,
		MERCENARIE,
		UNKNOWN
	}
	
	private final List<DropCategory> _categories = new LinkedList<>();
	private final List<L2MinionData> _minions = new ArrayList<>();
	private final List<ClassId> _teachInfo = new ArrayList<>();
	private final Map<Integer, L2Skill> _skills = new HashMap<>();
	private final Map<EventType, List<Quest>> _questEvents = new HashMap<>();
	
	/**
	 * Constructor of L2NpcTemplate.
	 * @param set The StatsSet object to transfer data to the method.
	 */
	public NpcTemplate(StatsSet set)
	{
		super(set);
		
		_npcId = set.getInteger("id");
		_idTemplate = set.getInteger("idTemplate", _npcId);
		
		_type = set.getString("type");
		
		_name = set.getString("name");
		_title = set.getString("title", "");
		
		_cantBeChampionMonster = (_title.equalsIgnoreCase("Quest Monster") || isType("L2Chest")) ? true : false;
		
		_level = set.getByte("level", (byte) 1);
		
		_exp = set.getInteger("exp", 0);
		_sp = set.getInteger("sp", 0);
		
		_rHand = set.getInteger("rHand", 0);
		_lHand = set.getInteger("lHand", 0);
		
		_enchantEffect = set.getInteger("enchant", 0);
		_corpseTime = set.getInteger("corpseTime", 7);
		_dropHerbGroup = set.getInteger("dropHerbGroup", 0);
		
		if (_dropHerbGroup > 0 && HerbDropTable.getInstance().getHerbDroplist(_dropHerbGroup) == null)
		{
			_log.warning("Missing dropHerbGroup information for npcId: " + _npcId + ", dropHerbGroup: " + _dropHerbGroup);
			_dropHerbGroup = 0;
		}
	}
	
	public void addTeachInfo(ClassId classId)
	{
		_teachInfo.add(classId);
	}
	
	public List<ClassId> getTeachInfo()
	{
		return _teachInfo;
	}
	
	public boolean canTeach(ClassId classId)
	{
		// If the player is on a third class, fetch the class teacher information for its parent class.
		if (classId.level() == 3)
			return _teachInfo.contains(classId.getParent());
		
		return _teachInfo.contains(classId);
	}
	
	// Add a drop to a given category. If the category does not exist, create it.
	public void addDropData(DropData drop, int categoryType)
	{
		// If the category doesn't already exist, create it first
		synchronized (_categories)
		{
			boolean catExists = false;
			for (DropCategory cat : _categories)
			{
				// If the category exists, add the drop to this category.
				if (cat.getCategoryType() == categoryType)
				{
					cat.addDropData(drop, isType("L2RaidBoss") || isType("L2GrandBoss"));
					catExists = true;
					break;
				}
			}
			
			// If the category doesn't exit, create it and add the drop
			if (!catExists)
			{
				DropCategory cat = new DropCategory(categoryType);
				cat.addDropData(drop, isType("L2RaidBoss") || isType("L2GrandBoss"));
				_categories.add(cat);
			}
		}
	}
	
	public void addRaidData(L2MinionData minion)
	{
		_minions.add(minion);
	}
	
	public void addSkill(L2Skill skill)
	{
		if (!skill.isPassive())
		{
			if (skill.isSuicideAttack())
				_suicideSkills.add(skill);
			else
			{
				switch (skill.getSkillType())
				{
					case BUFF:
					case CONT:
					case REFLECT:
						_buffSkills.add(skill);
						break;
					
					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case HEAL_STATIC:
					case BALANCE_LIFE:
					case MANARECHARGE:
					case MANAHEAL_PERCENT:
						_healSkills.add(skill);
						break;
					
					case DEBUFF:
					case ROOT:
					case SLEEP:
					case STUN:
					case PARALYZE:
					case POISON:
					case DOT:
					case MDOT:
					case BLEED:
					case MUTE:
					case FEAR:
					case CANCEL:
					case NEGATE:
					case WEAKNESS:
					case AGGDEBUFF:
						_debuffSkills.add(skill);
						break;
					
					case PDAM:
					case MDAM:
					case BLOW:
					case DRAIN:
					case CHARGEDAM:
					case FATAL:
					case DEATHLINK:
					case MANADAM:
					case CPDAMPERCENT:
					case GET_PLAYER:
					case INSTANT_JUMP:
					case AGGDAMAGE:
						addShortOrLongRangeSkill(skill);
						break;
				}
			}
		}
		_skills.put(skill.getId(), skill);
	}
	
	/**
	 * @return the list of all possible UNCATEGORIZED drops of this L2NpcTemplate.
	 */
	public List<DropCategory> getDropData()
	{
		return _categories;
	}
	
	/**
	 * @return the list of all possible item drops of this L2NpcTemplate. (ie full drops and part drops, mats, miscellaneous & UNCATEGORIZED)
	 */
	public List<DropData> getAllDropData()
	{
		final List<DropData> list = new ArrayList<>();
		for (DropCategory tmp : _categories)
			list.addAll(tmp.getAllDrops());
		
		return list;
	}
	
	/**
	 * @return the list of all Minions that must be spawn with the L2Npc using this L2NpcTemplate.
	 */
	public List<L2MinionData> getMinionData()
	{
		return _minions;
	}
	
	public Map<Integer, L2Skill> getSkills()
	{
		return _skills;
	}
	
	public L2Skill[] getSkillsArray()
	{
		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}
	
	public void addQuestEvent(EventType eventType, Quest quest)
	{
		List<Quest> eventList = _questEvents.get(eventType);
		if (eventList == null)
		{
			eventList = new ArrayList<>();
			eventList.add(quest);
			_questEvents.put(eventType, eventList);
		}
		else
		{
			eventList.remove(quest);
			
			if (eventType.isMultipleRegistrationAllowed() || eventList.isEmpty())
				eventList.add(quest);
			else
				_log.warning("Quest event not allow multiple quest registrations. Skipped addition of EventType \"" + eventType + "\" for NPC \"" + getName() + "\" and quest \"" + quest.getName() + "\".");
		}
	}
	
	public Map<EventType, List<Quest>> getEventQuests()
	{
		return _questEvents;
	}
	
	public List<Quest> getEventQuests(EventType EventType)
	{
		return _questEvents.get(EventType);
	}
	
	public void setRace(int raceId)
	{
		switch (raceId)
		{
			case 1:
				_race = Race.UNDEAD;
				break;
			case 2:
				_race = Race.MAGICCREATURE;
				break;
			case 3:
				_race = Race.BEAST;
				break;
			case 4:
				_race = Race.ANIMAL;
				break;
			case 5:
				_race = Race.PLANT;
				break;
			case 6:
				_race = Race.HUMANOID;
				break;
			case 7:
				_race = Race.SPIRIT;
				break;
			case 8:
				_race = Race.ANGEL;
				break;
			case 9:
				_race = Race.DEMON;
				break;
			case 10:
				_race = Race.DRAGON;
				break;
			case 11:
				_race = Race.GIANT;
				break;
			case 12:
				_race = Race.BUG;
				break;
			case 13:
				_race = Race.FAIRIE;
				break;
			case 14:
				_race = Race.HUMAN;
				break;
			case 15:
				_race = Race.ELVE;
				break;
			case 16:
				_race = Race.DARKELVE;
				break;
			case 17:
				_race = Race.ORC;
				break;
			case 18:
				_race = Race.DWARVE;
				break;
			case 19:
				_race = Race.OTHER;
				break;
			case 20:
				_race = Race.NONLIVING;
				break;
			case 21:
				_race = Race.SIEGEWEAPON;
				break;
			case 22:
				_race = Race.DEFENDINGARMY;
				break;
			case 23:
				_race = Race.MERCENARIE;
				break;
			default:
				_race = Race.UNKNOWN;
				break;
		}
	}
	
	// -----------------------------------------------------------------------
	// Getters
	
	/**
	 * @return the npc id.
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * @return the npc name.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the npc name.
	 */
	public String getTitle()
	{
		return _title;
	}
	
	/**
	 * @return the npc race.
	 */
	public Race getRace()
	{
		if (_race == null)
			_race = Race.UNKNOWN;
		
		return _race;
	}
	
	public String getType()
	{
		return _type;
	}
	
	/**
	 * @return the reward Exp.
	 */
	public int getRewardExp()
	{
		return _exp;
	}
	
	/**
	 * @return the reward SP.
	 */
	public int getRewardSp()
	{
		return _sp;
	}
	
	/**
	 * @return the right hand weapon.
	 */
	public int getRightHand()
	{
		return _rHand;
	}
	
	/**
	 * @return the right hand weapon.
	 */
	public int getLeftHand()
	{
		return _lHand;
	}
	
	/**
	 * @return the NPC level.
	 */
	public byte getLevel()
	{
		return _level;
	}
	
	/**
	 * @return the drop herb group.
	 */
	public int getDropHerbGroup()
	{
		return _dropHerbGroup;
	}
	
	/**
	 * @return the enchant effect.
	 */
	public int getEnchantEffect()
	{
		return _enchantEffect;
	}
	
	/**
	 * @return the Id template.
	 */
	public int getIdTemplate()
	{
		return _idTemplate;
	}
	
	/**
	 * @return the corpse decay time of the template.
	 */
	public int getCorpseTime()
	{
		return _corpseTime;
	}
	
	// -----------------------------------------------------------------------
	// Npc AI Data By ShanSoft
	
	public void setAIData(L2NpcAIData aidata)
	{
		_AIdata = aidata;
	}
	
	public L2NpcAIData getAIData()
	{
		return _AIdata;
	}
	
	public void addShortOrLongRangeSkill(L2Skill skill)
	{
		if (skill.getCastRange() > 150)
			_longRangeSkills.add(skill);
		else if (skill.getCastRange() > 0)
			_shortRangeSkills.add(skill);
	}
	
	public List<L2Skill> getSuicideSkills()
	{
		return _suicideSkills;
	}
	
	public List<L2Skill> getHealSkills()
	{
		return _healSkills;
	}
	
	public List<L2Skill> getDebuffSkills()
	{
		return _debuffSkills;
	}
	
	public List<L2Skill> getBuffSkills()
	{
		return _buffSkills;
	}
	
	public List<L2Skill> getLongRangeSkills()
	{
		return _longRangeSkills;
	}
	
	public List<L2Skill> getShortRangeSkills()
	{
		return _shortRangeSkills;
	}
	
	// -----------------------------------------------------------------------
	// Misc
	
	public boolean isSpecialTree()
	{
		return _npcId == L2XmassTreeInstance.SPECIAL_TREE_ID;
	}
	
	public boolean isUndead()
	{
		return _race == Race.UNDEAD;
	}
	
	public boolean cantBeChampion()
	{
		return _cantBeChampionMonster;
	}
	
	public boolean isCustomNpc()
	{
		return getNpcId() != getIdTemplate();
	}
	
	/**
	 * Checks types, ignore case.
	 * @param t the type to check.
	 * @return true if the type are the same, false otherwise.
	 */
	public boolean isType(String t)
	{
		return _type.equalsIgnoreCase(t);
	}
}