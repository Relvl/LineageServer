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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.network.clientpackets.AbstractRefinePacket;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class manages the augmentation data and can also create new augmentations.
 * @author durgus, edited by Gigiikun
 */
public class AugmentationData
{
	private static final Logger _log = Logger.getLogger(AugmentationData.class.getName());
	
	public static final AugmentationData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	// stats
	private static final int STAT_START = 1;
	private static final int STAT_END = 14560;
	private static final int STAT_BLOCKSIZE = 3640;
	private static final int STAT_SUBBLOCKSIZE = 91;
	private static final int STAT_NUM = 13;
	
	private static final byte[] STATS1_MAP = new byte[STAT_SUBBLOCKSIZE];
	private static final byte[] STATS2_MAP = new byte[STAT_SUBBLOCKSIZE];
	
	// skills
	private static final int BLUE_START = 14561;
	private static final int SKILLS_BLOCKSIZE = 178;
	
	// basestats
	private static final int BASESTAT_STR = 16341;
	private static final int BASESTAT_CON = 16342;
	private static final int BASESTAT_INT = 16343;
	private static final int BASESTAT_MEN = 16344;
	
	private final ArrayList<?>[] _augStats = new ArrayList[4];
	
	private final ArrayList<?>[] _blueSkills = new ArrayList[10];
	private final ArrayList<?>[] _purpleSkills = new ArrayList[10];
	private final ArrayList<?>[] _redSkills = new ArrayList[10];
	
	private final Map<Integer, AugmentationSkill> _allSkills = new HashMap<>();
	
	protected AugmentationData()
	{
		_augStats[0] = new ArrayList<AugmentationStat>();
		_augStats[1] = new ArrayList<AugmentationStat>();
		_augStats[2] = new ArrayList<AugmentationStat>();
		_augStats[3] = new ArrayList<AugmentationStat>();
		
		// Lookup tables structure: STAT1 represent first stat, STAT2 - second.
		// If both values are the same - use solo stat, if different - combined.
		byte idx;
		
		// weapon augmentation block: solo values first
		for (idx = 0; idx < STAT_NUM; idx++)
		{
			// solo stats
			STATS1_MAP[idx] = idx;
			STATS2_MAP[idx] = idx;
		}
		
		// combined values next.
		for (int i = 0; i < STAT_NUM; i++)
		{
			for (int j = i + 1; j < STAT_NUM; idx++, j++)
			{
				// combined stats
				STATS1_MAP[idx] = (byte) i;
				STATS2_MAP[idx] = (byte) j;
			}
		}
		
		for (int i = 0; i < 10; i++)
		{
			_blueSkills[i] = new ArrayList<Integer>();
			_purpleSkills[i] = new ArrayList<Integer>();
			_redSkills[i] = new ArrayList<Integer>();
		}
		
		load();
		
		// Use size*4: since theres 4 blocks of stat-data with equivalent size
		_log.info("AugmentationData: Loaded " + _augStats.length + " sets of " + (_augStats[0].size() * 4) + " augmentation stats.");
		
		int blue = 0, purple = 0, red = 0;
		for (int i = 0; i < 10; i++)
		{
			blue += _blueSkills[i].size();
			purple += _purpleSkills[i].size();
			red += _redSkills[i].size();
		}
		_log.info("AugmentationData: Loaded " + blue + " blue, " + purple + " purple and " + red + " red Life-Stone skills.");
	}
	
	public static class AugmentationSkill
	{
		private final int _skillId;
		private final int _skillLevel;
		
		public AugmentationSkill(int skillId, int skillLevel)
		{
			_skillId = skillId;
			_skillLevel = skillLevel;
		}
		
		public L2Skill getSkill()
		{
			return SkillTable.getInstance().getInfo(_skillId, _skillLevel);
		}
	}
	
	public static class AugmentationStat
	{
		private final Stats _stat;
		private final int _singleSize;
		private final int _combinedSize;
		private final float _singleValues[];
		private final float _combinedValues[];
		
		public AugmentationStat(Stats stat, float sValues[], float cValues[])
		{
			_stat = stat;
			_singleSize = sValues.length;
			_singleValues = sValues;
			_combinedSize = cValues.length;
			_combinedValues = cValues;
		}
		
		public int getSingleStatSize()
		{
			return _singleSize;
		}
		
		public int getCombinedStatSize()
		{
			return _combinedSize;
		}
		
		public float getSingleStatValue(int i)
		{
			if (i >= _singleSize || i < 0)
				return _singleValues[_singleSize - 1];
			
			return _singleValues[i];
		}
		
		public float getCombinedStatValue(int i)
		{
			if (i >= _combinedSize || i < 0)
				return _combinedValues[_combinedSize - 1];
			
			return _combinedValues[i];
		}
		
		public Stats getStat()
		{
			return _stat;
		}
	}
	
	@SuppressWarnings("unchecked")
	private final void load()
	{
		// Load the skillmap
		try
		{
			int badAugmantData = 0;
			
			File file = new File("./data/xml/augmentation/augmentation_skillmap.xml");
			final Document doc = XMLDocumentFactory.getInstance().loadDocument(file);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("augmentation".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int skillId = 0, augmentationId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							int skillLvL = 0;
							String type = "blue";
							
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("skillId".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillId = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if ("skillLevel".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillLvL = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if ("type".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									type = attrs.getNamedItem("val").getNodeValue();
								}
							}
							if (skillId == 0)
							{
								if (Config.DEBUG)
									_log.log(Level.SEVERE, "AugmentationData: Bad skillId in augmentation_skillmap.xml for id:" + augmentationId);
								badAugmantData++;
								continue;
							}
							else if (skillLvL == 0)
							{
								if (Config.DEBUG)
									_log.log(Level.SEVERE, "AugmentationData: Bad skillLevel in augmentation_skillmap.xml for id:" + augmentationId);
								badAugmantData++;
								continue;
							}
							
							int k = (augmentationId - BLUE_START) / SKILLS_BLOCKSIZE;
							
							if (type.equalsIgnoreCase("blue"))
								((ArrayList<Integer>) _blueSkills[k]).add(augmentationId);
							else if (type.equalsIgnoreCase("purple"))
								((ArrayList<Integer>) _purpleSkills[k]).add(augmentationId);
							else
								((ArrayList<Integer>) _redSkills[k]).add(augmentationId);
							
							_allSkills.put(augmentationId, new AugmentationSkill(skillId, skillLvL));
						}
					}
				}
			}
			if (badAugmantData != 0)
				_log.info("AugmentationData: " + badAugmantData + " bad skill(s) were skipped.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "AugmentationData: Error parsing augmentation_skillmap.xml: ", e);
			return;
		}
		
		// Load the stats from xml
		for (int i = 1; i < 5; i++)
		{
			try
			{
				File file = new File("./data/xml/augmentation/augmentation_stats" + i + ".xml");
				final Document doc = XMLDocumentFactory.getInstance().loadDocument(file);
				
				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								String statName = attrs.getNamedItem("name").getNodeValue();
								float soloValues[] = null, combinedValues[] = null;
								
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("table".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String tableName = attrs.getNamedItem("name").getNodeValue();
										
										StringTokenizer data = new StringTokenizer(cd.getFirstChild().getNodeValue());
										List<Float> array = new ArrayList<>();
										while (data.hasMoreTokens())
											array.add(Float.parseFloat(data.nextToken()));
										
										if (tableName.equalsIgnoreCase("#soloValues"))
										{
											soloValues = new float[array.size()];
											int x = 0;
											for (float value : array)
												soloValues[x++] = value;
										}
										else
										{
											combinedValues = new float[array.size()];
											int x = 0;
											for (float value : array)
												combinedValues[x++] = value;
										}
									}
								}
								// store this stat
								((ArrayList<AugmentationStat>) _augStats[(i - 1)]).add(new AugmentationStat(Stats.valueOfXml(statName), soloValues, combinedValues));
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "AugmentationData: Error parsing augmentation_stats" + i + ".xml.", e);
				return;
			}
		}
	}
	
	public L2Augmentation generateRandomAugmentation(int lifeStoneLevel, int lifeStoneGrade)
	{
		// Note that stat12 stands for stat 1 AND 2 (same for stat34 ;p )
		// this is because a value can contain up to 2 stat modifications
		// (there are two short values packed in one integer value, meaning 4 stat modifications at max)
		// for more info take a look at getAugStatsById(...)
		
		// Note: lifeStoneGrade: (0 means low grade, 3 top grade)
		// First: determine whether we will add a skill/baseStatModifier or not
		// because this determine which color could be the result
		int stat12 = 0;
		int stat34 = 0;
		boolean generateSkill = false;
		boolean generateGlow = false;
		
		// lifestonelevel is used for stat Id and skill level, but here the max level is 9
		lifeStoneLevel = Math.min(lifeStoneLevel, 9);
		
		switch (lifeStoneGrade)
		{
			case AbstractRefinePacket.GRADE_NONE:
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_NG_SKILL_CHANCE)
					generateSkill = true;
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_NG_GLOW_CHANCE)
					generateGlow = true;
				break;
			case AbstractRefinePacket.GRADE_MID:
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_MID_SKILL_CHANCE)
					generateSkill = true;
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_MID_GLOW_CHANCE)
					generateGlow = true;
				break;
			case AbstractRefinePacket.GRADE_HIGH:
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_HIGH_SKILL_CHANCE)
					generateSkill = true;
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_HIGH_GLOW_CHANCE)
					generateGlow = true;
				break;
			case AbstractRefinePacket.GRADE_TOP:
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_TOP_SKILL_CHANCE)
					generateSkill = true;
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_TOP_GLOW_CHANCE)
					generateGlow = true;
				break;
		}
		
		if (!generateSkill && Rnd.get(1, 100) <= Config.AUGMENTATION_BASESTAT_CHANCE)
			stat34 = Rnd.get(BASESTAT_STR, BASESTAT_MEN);
		
		// Second: decide which grade the augmentation result is going to have:
		// 0:yellow, 1:blue, 2:purple, 3:red
		// The chances used here are most likely custom,
		// whats known is: you cant have yellow with skill(or baseStatModifier)
		// noGrade stone can not have glow, mid only with skill, high has a chance(custom), top allways glow
		int resultColor = Rnd.get(0, 100);
		if (stat34 == 0 && !generateSkill)
		{
			if (resultColor <= (15 * lifeStoneGrade) + 40)
				resultColor = 1;
			else
				resultColor = 0;
		}
		else
		{
			if (resultColor <= (10 * lifeStoneGrade) + 5 || stat34 != 0)
				resultColor = 3;
			else if (resultColor <= (10 * lifeStoneGrade) + 10)
				resultColor = 1;
			else
				resultColor = 2;
		}
		
		// generate a skill if neccessary
		L2Skill skill = null;
		if (generateSkill)
		{
			switch (resultColor)
			{
				case 1: // blue skill
					stat34 = ((Integer) _blueSkills[lifeStoneLevel].get(Rnd.get(0, _blueSkills[lifeStoneLevel].size() - 1)));
					break;
				case 2: // purple skill
					stat34 = ((Integer) _purpleSkills[lifeStoneLevel].get(Rnd.get(0, _purpleSkills[lifeStoneLevel].size() - 1)));
					break;
				case 3: // red skill
					stat34 = ((Integer) _redSkills[lifeStoneLevel].get(Rnd.get(0, _redSkills[lifeStoneLevel].size() - 1)));
					break;
			}
			skill = _allSkills.get(stat34).getSkill();
		}
		
		// Third: Calculate the subblock offset for the choosen color,
		// and the level of the lifeStone
		// from large number of retail augmentations:
		// no skill part
		// Id for stat12:
		// A:1-910 B:911-1820 C:1821-2730 D:2731-3640 E:3641-4550 F:4551-5460 G:5461-6370 H:6371-7280
		// Id for stat34(this defines the color):
		// I:7281-8190(yellow) K:8191-9100(blue) L:10921-11830(yellow) M:11831-12740(blue)
		// you can combine I-K with A-D and L-M with E-H
		// using C-D or G-H Id you will get a glow effect
		// there seems no correlation in which grade use which Id except for the glowing restriction
		// skill part
		// Id for stat12:
		// same for no skill part
		// A same as E, B same as F, C same as G, D same as H
		// A - no glow, no grade LS
		// B - weak glow, mid grade LS?
		// C - glow, high grade LS?
		// D - strong glow, top grade LS?
		
		// is neither a skill nor basestat used for stat34? then generate a normal stat
		int offset;
		if (stat34 == 0)
		{
			int temp = Rnd.get(2, 3);
			int colorOffset = resultColor * (10 * STAT_SUBBLOCKSIZE) + temp * STAT_BLOCKSIZE + 1;
			offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + colorOffset;
			
			stat34 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
			if (generateGlow && lifeStoneGrade >= 2)
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + (temp - 2) * STAT_BLOCKSIZE + lifeStoneGrade * (10 * STAT_SUBBLOCKSIZE) + 1;
			else
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + (temp - 2) * STAT_BLOCKSIZE + Rnd.get(0, 1) * (10 * STAT_SUBBLOCKSIZE) + 1;
		}
		else
		{
			if (!generateGlow)
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + Rnd.get(0, 1) * STAT_BLOCKSIZE + 1;
			else
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + Rnd.get(0, 1) * STAT_BLOCKSIZE + (lifeStoneGrade + resultColor) / 2 * (10 * STAT_SUBBLOCKSIZE) + 1;
		}
		stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		
		if (Config.DEBUG)
			_log.info("Augmentation success: stat12=" + stat12 + "; stat34=" + stat34 + "; resultColor=" + resultColor + "; level=" + lifeStoneLevel + "; grade=" + lifeStoneGrade);
		
		return new L2Augmentation(((stat34 << 16) + stat12), skill);
	}
	
	public static class AugStat
	{
		private final Stats _stat;
		private final float _value;
		
		public AugStat(Stats stat, float value)
		{
			_stat = stat;
			_value = value;
		}
		
		public Stats getStat()
		{
			return _stat;
		}
		
		public float getValue()
		{
			return _value;
		}
	}
	
	/**
	 * Returns the stat and basestat boni for a given augmentation id
	 * @param augmentationId
	 * @return
	 */
	public List<AugStat> getAugStatsById(int augmentationId)
	{
		List<AugStat> temp = new ArrayList<>();
		// An augmentation id contains 2 short vaues so we gotta seperate them here
		// both values contain a number from 1-16380, the first 14560 values are stats
		// the 14560 stats are divided into 4 blocks each holding 3640 values
		// each block contains 40 subblocks holding 91 stat values
		// the first 13 values are so called Solo-stats and they have the highest stat increase possible
		// after the 13 Solo-stats come 78 combined stats (thats every possible combination of the 13 solo stats)
		// the first 12 combined stats (14-26) is the stat 1 combined with stat 2-13
		// the next 11 combined stats then are stat 2 combined with stat 3-13 and so on...
		// to get the idea have a look @ optiondata_client-e.dat - thats where the data came from :)
		int stats[] = new int[2];
		stats[0] = 0x0000FFFF & augmentationId;
		stats[1] = (augmentationId >> 16);
		
		for (int i = 0; i < 2; i++)
		{
			// weapon augmentation - stats
			if (stats[i] >= STAT_START && stats[i] <= STAT_END)
			{
				int base = stats[i] - STAT_START;
				int color = base / STAT_BLOCKSIZE; // 4 color blocks
				int subblock = base % STAT_BLOCKSIZE; // offset in color block
				int level = subblock / STAT_SUBBLOCKSIZE; // stat level (sub-block number)
				int stat = subblock % STAT_SUBBLOCKSIZE; // offset in sub-block - stat
				
				byte stat1 = STATS1_MAP[stat];
				byte stat2 = STATS2_MAP[stat];
				if (stat1 == stat2) // solo stat
				{
					AugmentationStat as = ((AugmentationStat) _augStats[color].get(stat1));
					temp.add(new AugStat(as.getStat(), as.getSingleStatValue(level)));
				}
				else
				// combined stat
				{
					AugmentationStat as = ((AugmentationStat) _augStats[color].get(stat1));
					temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(level)));
					as = ((AugmentationStat) _augStats[color].get(stat2));
					temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(level)));
				}
			}
			// its a base stat
			else if (stats[i] >= BASESTAT_STR && stats[i] <= BASESTAT_MEN)
			{
				switch (stats[i])
				{
					case BASESTAT_STR:
						temp.add(new AugStat(Stats.STAT_STR, 1.0f));
						break;
					case BASESTAT_CON:
						temp.add(new AugStat(Stats.STAT_CON, 1.0f));
						break;
					case BASESTAT_INT:
						temp.add(new AugStat(Stats.STAT_INT, 1.0f));
						break;
					case BASESTAT_MEN:
						temp.add(new AugStat(Stats.STAT_MEN, 1.0f));
						break;
				}
			}
		}
		return temp;
	}
	
	private static class SingletonHolder
	{
		protected static final AugmentationData _instance = new AugmentationData();
	}
}