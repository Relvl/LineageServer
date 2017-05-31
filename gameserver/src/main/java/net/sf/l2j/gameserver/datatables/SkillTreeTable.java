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
package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2EnchantSkillData;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2PledgeSkillLearn;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SkillTreeTable
{
	private static Logger _log = Logger.getLogger(SkillTreeTable.class.getName());
	
	private Map<ClassId, Map<Integer, L2SkillLearn>> _skillTrees;
	private List<L2SkillLearn> _fishingSkillTrees;
	private List<L2SkillLearn> _expandDwarvenCraftSkillTrees;
	private List<L2PledgeSkillLearn> _pledgeSkillTrees;
	private Map<Integer, L2EnchantSkillData> _enchantSkillData;
	private List<L2EnchantSkillLearn> _enchantSkillTrees;
	
	public static SkillTreeTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected SkillTreeTable()
	{
		load();
	}
	
	private void load()
	{
		// / General skills tree
		_skillTrees = new HashMap<>();
		
		// Fishing skills tree && Expand dwarven craft skills tree
		try
		{
			_fishingSkillTrees = new ArrayList<>();
			_expandDwarvenCraftSkillTrees = new ArrayList<>();
			
			File f = new File("./data/xml/skillstrees/fishing_skills_tree.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			for (Node list = doc.getFirstChild().getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("skill".equalsIgnoreCase(list.getNodeName()))
				{
					NamedNodeMap attrs = list.getAttributes();
					
					int id = Integer.valueOf(attrs.getNamedItem("id").getNodeValue());
					int lvl = Integer.valueOf(attrs.getNamedItem("lvl").getNodeValue());
					int minLvl = Integer.parseInt(attrs.getNamedItem("minLvl").getNodeValue());
					int itemId = Integer.parseInt(attrs.getNamedItem("itemId").getNodeValue());
					int itemCount = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
					
					if (Boolean.parseBoolean(attrs.getNamedItem("isDwarf").getNodeValue()))
						_expandDwarvenCraftSkillTrees.add(new L2SkillLearn(id, lvl, minLvl, 0, itemId, itemCount));
					else
						_fishingSkillTrees.add(new L2SkillLearn(id, lvl, minLvl, 0, itemId, itemCount));
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("FishingTable: Error while loading fishing skills: " + e);
		}
		
		// Enchant skills tree && Enchant data
		try
		{
			_enchantSkillData = new LinkedHashMap<>();
			_enchantSkillTrees = new ArrayList<>();
			
			File f = new File("./data/xml/skillstrees/enchant_skills_tree.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			for (Node list = doc.getFirstChild().getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("enchanttype".equalsIgnoreCase(list.getNodeName()))
				{
					for (Node type = list.getFirstChild(); type != null; type = type.getNextSibling())
					{
						if ("data".equalsIgnoreCase(type.getNodeName()))
						{
							NamedNodeMap dataAttrs = type.getAttributes();
							
							int enchant = Integer.valueOf(dataAttrs.getNamedItem("enchant").getNodeValue());
							int exp = Integer.valueOf(dataAttrs.getNamedItem("exp").getNodeValue());
							int sp = Integer.valueOf(dataAttrs.getNamedItem("sp").getNodeValue());
							int itemId = 0;
							int itemCount = 0;
							
							Node att = dataAttrs.getNamedItem("itemId");
							if (att != null)
							{
								itemId = Integer.valueOf(att.getNodeValue());
								att = dataAttrs.getNamedItem("itemCount");
								itemCount = (att == null) ? 1 : Integer.valueOf(att.getNodeValue());
							}
							
							int rate76 = Integer.valueOf(dataAttrs.getNamedItem("rate_76").getNodeValue());
							int rate77 = Integer.valueOf(dataAttrs.getNamedItem("rate_77").getNodeValue());
							int rate78 = Integer.valueOf(dataAttrs.getNamedItem("rate_78").getNodeValue());
							
							_enchantSkillData.put(enchant, new L2EnchantSkillData(exp, sp, itemId, itemCount, rate76, rate77, rate78));
						}
					}
				}
				else if ("enchant".equalsIgnoreCase(list.getNodeName()))
				{
					NamedNodeMap enchantAttrs = list.getAttributes();
					
					int id = Integer.valueOf(enchantAttrs.getNamedItem("id").getNodeValue());
					int baseLvl = Integer.valueOf(enchantAttrs.getNamedItem("baseLvl").getNodeValue());
					
					for (Node skill = list.getFirstChild(); skill != null; skill = skill.getNextSibling())
					{
						if ("data".equalsIgnoreCase(skill.getNodeName()))
						{
							NamedNodeMap dataAttrs = skill.getAttributes();
							
							int lvl = Integer.valueOf(dataAttrs.getNamedItem("level").getNodeValue());
							int enchant = Integer.valueOf(dataAttrs.getNamedItem("enchant").getNodeValue());
							
							int prevLvl = lvl - 1;
							if (lvl == 101 || lvl == 141)
								prevLvl = baseLvl;
							
							_enchantSkillTrees.add(new L2EnchantSkillLearn(id, lvl, baseLvl, prevLvl, enchant));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.severe("EnchantSkillTable: Error while loading enchant skills tree: " + e);
		}
		
		// Pledge skills tree
		try
		{
			_pledgeSkillTrees = new ArrayList<>();
			
			File f = new File("./data/xml/skillstrees/pledge_skills_tree.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			for (Node list = doc.getFirstChild().getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("clan".equals(list.getNodeName()))
				{
					int clanLvl = Integer.parseInt(list.getAttributes().getNamedItem("lvl").getNodeValue());
					
					for (Node clan = list.getFirstChild(); clan != null; clan = clan.getNextSibling())
					{
						if ("skill".equals(clan.getNodeName()))
						{
							NamedNodeMap skillAttr = clan.getAttributes();
							
							int skillId = Integer.parseInt(skillAttr.getNamedItem("id").getNodeValue());
							int skillLvl = Integer.parseInt(skillAttr.getNamedItem("lvl").getNodeValue());
							int repCost = Integer.parseInt(skillAttr.getNamedItem("repCost").getNodeValue());
							int itemId = 0;
							
							Node att = skillAttr.getNamedItem("itemId");
							if (att != null)
								itemId = Integer.valueOf(att.getNodeValue());
							
							_pledgeSkillTrees.add(new L2PledgeSkillLearn(skillId, skillLvl, clanLvl, repCost, itemId));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.severe("PledgeTable: Error while loading pledge skills: " + e);
		}
		
		_log.config("FishingSkillTreeTable: Loaded " + _fishingSkillTrees.size() + " general skills.");
		_log.config("DwarvenCraftSkillTreeTable: Loaded " + _expandDwarvenCraftSkillTrees.size() + " dwarven skills.");
		_log.config("EnchantSkillTreeTable: Loaded " + _enchantSkillData.size() + " enchant types and " + _enchantSkillTrees.size() + " enchant skills.");
		_log.config("PledgeSkillTreeTable: Loaded " + _pledgeSkillTrees.size() + " pledge skills.");
	}
	
	/**
	 * Adds list of skills to general skill tree according to classId.
	 * @param skills List of general skills to be added.
	 * @param classId ClassId of skill owner.
	 * @param parentId Parent id of skill owner.
	 */
	public void addSkillsToSkillTrees(final List<L2SkillLearn> skills, final int classId, final int parentId)
	{
		if (skills == null || skills.isEmpty())
			return;
		
		Map<Integer, L2SkillLearn> tmp = new LinkedHashMap<>();
		
		if (parentId > -1)
		{
			Map<Integer, L2SkillLearn> parent = _skillTrees.get(ClassId.values()[parentId]);
			
			if (parent != null)
				for (L2SkillLearn skillLearn : parent.values())
					if (skillLearn != null)
						tmp.put(SkillTable.getSkillHashCode(skillLearn.getId(), skillLearn.getLevel()), skillLearn);
		}
		
		for (L2SkillLearn skillLearn : skills)
			if (skillLearn != null)
				tmp.put(SkillTable.getSkillHashCode(skillLearn.getId(), skillLearn.getLevel()), skillLearn);
		
		if (!tmp.isEmpty())
			_skillTrees.put(ClassId.values()[classId], tmp);
	}
	
	/**
	 * Returns size of general skills tree (amount of classes with general skills).
	 * @return int : Size of general skill tree.
	 */
	public int getSkillTreesSize()
	{
		return _skillTrees.size();
	}
	
	/**
	 * @param cha L2PcInstance player whom skills are compared.
	 * @param classId ClassId as a source for skill tree.
	 * @return list of available general skills for L2PcInstance.
	 */
	public List<L2SkillLearn> getAvailableSkills(L2PcInstance cha, ClassId classId)
	{
		List<L2SkillLearn> result = new ArrayList<>();
		
		L2Skill[] chaSkills = cha.getAllSkills();
		int level = cha.getLevel();
		
		for (L2SkillLearn sl : _skillTrees.get(classId).values())
		{
			// Exception for Lucky skill, it can't be learned back once lost.
			if (sl.getId() == L2Skill.SKILL_LUCKY)
				continue;
			
			if (sl.getMinLevel() <= level)
			{
				boolean found = false;
				
				for (L2Skill s : chaSkills)
				{
					if (s.getId() == sl.getId())
					{
						// this is the next level of a skill that we know
						if (s.getLevel() == sl.getLevel() - 1)
							result.add(sl);
						
						found = true;
						break;
					}
				}
				
				if (!found && sl.getLevel() == 1)
					result.add(sl);
			}
		}
		
		return result;
	}
	
	/**
	 * @param cha L2PcInstance, player whom skills are compared.
	 * @param classId ClassId, as a source for skill tree.
	 * @return list of all available general skills <b>of maximal level</b> for L2PcInstance.
	 */
	public Collection<L2SkillLearn> getAllAvailableSkills(L2PcInstance cha, ClassId classId)
	{
		Map<Integer, L2SkillLearn> result = new LinkedHashMap<>();
		
		int skillId, level = cha.getLevel();
		L2SkillLearn skill;
		
		for (L2SkillLearn sl : _skillTrees.get(classId).values())
		{
			skillId = sl.getId();
			// Exception for Lucky skill, it can't be learned back once lost.
			if (skillId == L2Skill.SKILL_LUCKY)
				continue;
			
			if (sl.getMinLevel() <= level)
			{
				skill = result.get(skillId);
				if (skill == null)
					result.put(skillId, sl);
				else if (sl.getLevel() > skill.getLevel())
					result.put(skillId, sl);
			}
		}
		for (L2Skill s : cha.getAllSkills())
		{
			skillId = s.getId();
			skill = result.get(skillId);
			if (skill != null)
				if (s.getLevel() >= skill.getLevel())
					result.remove(skillId);
		}
		return result.values();
	}
	
	/**
	 * @param cha L2PcInstance, player whom level is checked.
	 * @param classId ClassId, as a source for skill tree.
	 * @return the minimum level for next general skill for L2PcInstance.
	 */
	public int getMinLevelForNewSkill(L2PcInstance cha, ClassId classId)
	{
		int level = cha.getLevel();
		int result = Integer.MAX_VALUE;
		
		for (L2SkillLearn sl : _skillTrees.get(classId).values())
		{
			int minLevel = sl.getMinLevel();
			if (minLevel > level && sl.getSpCost() != 0)
				if (minLevel < result)
					result = minLevel;
		}
		
		if (result == Integer.MAX_VALUE)
			return 0;
		
		return result;
	}
	
	/**
	 * @param classId ClassId, as a source for skill tree.
	 * @return the collection of general skill for given classId.
	 */
	public Collection<L2SkillLearn> getAllowedSkills(ClassId classId)
	{
		return _skillTrees.get(classId).values();
	}
	
	/**
	 * @param cha L2PcInstance, player whom skills are compared.
	 * @return list of available fishing and expand dwarven craft skills for L2PcInstance.
	 */
	public List<L2SkillLearn> getAvailableFishingDwarvenCraftSkills(L2PcInstance cha)
	{
		List<L2SkillLearn> result = new ArrayList<>();
		List<L2SkillLearn> skills = new ArrayList<>();
		
		skills.addAll(_fishingSkillTrees);
		if (cha.hasDwarvenCraft())
			skills.addAll(_expandDwarvenCraftSkillTrees);
		
		L2Skill[] chaSkills = cha.getAllSkills();
		int level = cha.getLevel();
		
		for (L2SkillLearn sl : skills)
		{
			if (sl.getMinLevel() <= level)
			{
				boolean found = false;
				
				for (L2Skill s : chaSkills)
				{
					if (s.getId() == sl.getId())
					{
						if (s.getLevel() == sl.getLevel() - 1)
							result.add(sl);
						
						found = true;
						break;
					}
				}
				
				if (!found && sl.getLevel() == 1)
					result.add(sl);
			}
		}
		return result;
	}
	
	/**
	 * @param cha L2PcInstance, player whom level is checked.
	 * @return the minimum level for next fishing and expand dwarven craft skill for L2PcInstance.
	 */
	public int getMinLevelForNewFishingDwarvenCraftSkill(L2PcInstance cha)
	{
		List<L2SkillLearn> skills = new ArrayList<>();
		
		skills.addAll(_fishingSkillTrees);
		if (cha.hasDwarvenCraft())
			skills.addAll(_expandDwarvenCraftSkillTrees);
		
		int level = cha.getLevel();
		int result = Integer.MAX_VALUE;
		
		for (L2SkillLearn sl : skills)
		{
			int minLvl = sl.getMinLevel();
			if (minLvl > level)
				if (minLvl < result)
					result = minLvl;
		}
		
		if (result == Integer.MAX_VALUE)
			return 0;
		
		return result;
	}
	
	/**
	 * @param cha L2PcInstance, player whom skills are compared.
	 * @return list of available enchant skills for L2PcInstance.
	 */
	public List<L2EnchantSkillLearn> getAvailableEnchantSkills(L2PcInstance cha)
	{
		List<L2EnchantSkillLearn> result = new ArrayList<>();
		
		L2Skill[] chaSkills = cha.getAllSkills();
		
		for (L2EnchantSkillLearn esl : _enchantSkillTrees)
		{
			for (L2Skill skill : chaSkills)
			{
				if (skill.getId() == esl.getId())
					if (skill.getLevel() == esl.getPrevLevel())
					{
						result.add(esl);
						break;
					}
			}
		}
		return result;
	}
	
	/**
	 * Get skill enchant data under L2EnchantSkillData model, or null if none data found.
	 * @param enchant the Enchant ID.
	 * @return L2EnchantSkillData corresponding to the given id or null.
	 */
	public L2EnchantSkillData getEnchantSkillData(int enchant)
	{
		return _enchantSkillData.get(enchant);
	}
	
	/**
	 * @param cha L2PcInstance, player whom skills are compared.
	 * @return list of available pledge skills for L2PcInstance.
	 */
	public List<L2PledgeSkillLearn> getAvailablePledgeSkills(L2PcInstance cha)
	{
		List<L2PledgeSkillLearn> result = new ArrayList<>();
		
		L2Skill[] clanSkills = cha.getClan().getClanSkills();
		int clanLvl = cha.getClan().getLevel();
		
		for (L2PledgeSkillLearn psl : _pledgeSkillTrees)
		{
			if (psl.getBaseLevel() <= clanLvl)
			{
				boolean found = false;
				
				for (L2Skill s : clanSkills)
				{
					if (s.getId() == psl.getId())
					{
						if (s.getLevel() == psl.getLevel() - 1)
							result.add(psl);
						
						found = true;
						break;
					}
				}
				
				if (!found && psl.getLevel() == 1)
					result.add(psl);
			}
		}
		return result;
	}
	
	public int getMinSkillLevel(int skillId, int skillLvl)
	{
		int skillHashCode = SkillTable.getSkillHashCode(skillId, skillLvl);
		
		for (Map<Integer, L2SkillLearn> map : _skillTrees.values())
		{
			if (map.containsKey(skillHashCode))
				return map.get(skillHashCode).getMinLevel();
		}
		return 0;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillTreeTable _instance = new SkillTreeTable();
	}
}