/*
 * his program is free software: you can redistribute it and/or modify it under
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.soulcrystal.LevelingInfo;
import net.sf.l2j.gameserver.model.soulcrystal.LevelingInfo.AbsorbCrystalType;
import net.sf.l2j.gameserver.model.soulcrystal.SoulCrystalData;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SoulCrystalsTable
{
	private static final Logger _log = Logger.getLogger(SoulCrystalsTable.class.getName());
	
	private static final Map<Integer, SoulCrystalData> _soulCrystals = new HashMap<>();
	private static final Map<Integer, LevelingInfo> _npcLevelingInfos = new HashMap<>();
	
	public static void load()
	{
		try
		{
			final File f = new File("./data/xml/soul_crystals.xml");
			final Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			final Node first = doc.getFirstChild();
			for (Node n = first.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("crystals".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("crystal".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att = attrs.getNamedItem("crystal");
							if (att == null)
							{
								_log.severe("SoulCrystalsTable: Missing \"crystal\" in \"soul_crystals.xml\", skipping.");
								continue;
							}
							int crystalItemId = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("level");
							if (att == null)
							{
								_log.severe("SoulCrystalsTable: Missing \"level\" in \"soul_crystals.xml\" crystal=" + crystalItemId + ", skipping.");
								continue;
							}
							int level = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("staged");
							if (att == null)
							{
								_log.severe("SoulCrystalsTable: Missing \"staged\" in \"soul_crystals.xml\" crystal=" + crystalItemId + ", skipping.");
								continue;
							}
							int stagedItemId = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("broken");
							if (att == null)
							{
								_log.severe("SoulCrystalsTable: Missing \"broken\" in \"soul_crystals.xml\" crystal=" + crystalItemId + ", skipping.");
								continue;
							}
							int brokenItemId = Integer.parseInt(att.getNodeValue());
							
							_soulCrystals.put(crystalItemId, new SoulCrystalData(level, crystalItemId, stagedItemId, brokenItemId));
						}
					}
				}
				else if ("npcs".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("npc".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att = attrs.getNamedItem("npcId");
							if (att == null)
							{
								_log.severe("SoulCrystalsTable: Missing \"npcId\" in \"soul_crystals.xml\", skipping.");
								continue;
							}
							int npcId = Integer.parseInt(att.getNodeValue());
							
							Node det = d.getFirstChild().getNextSibling();
							if (det.getNodeName().equals("detail"))
							{
								attrs = det.getAttributes();
								
								boolean skillRequired = false;
								att = attrs.getNamedItem("skill");
								if (att != null)
									skillRequired = Boolean.parseBoolean(att.getNodeValue());
								
								int chanceStage = 10;
								att = attrs.getNamedItem("chanceStage");
								if (att != null)
									chanceStage = Integer.parseInt(att.getNodeValue());
								
								int chanceBreak = 0;
								att = attrs.getNamedItem("chanceBreak");
								if (att != null)
									chanceBreak = Integer.parseInt(att.getNodeValue());
								
								AbsorbCrystalType absorbType = AbsorbCrystalType.LAST_HIT;
								att = attrs.getNamedItem("absorbType");
								if (att != null)
									absorbType = Enum.valueOf(AbsorbCrystalType.class, att.getNodeValue());
								
								int[] levelList = null;
								att = attrs.getNamedItem("levelList");
								if (att != null)
								{
									String str[] = att.getNodeValue().split(",");
									levelList = new int[str.length];
									for (int i = 0; i < str.length; i++)
									{
										Integer value = Integer.parseInt(str[i].trim());
										if (value == null)
										{
											_log.severe("SoulCrystalsTable: Bad level value for npcId=" + npcId + ", token=" + str[i]);
											continue;
										}
										levelList[i] = value;
									}
								}
								
								_npcLevelingInfos.put(npcId, new LevelingInfo(absorbType, skillRequired, chanceStage, chanceBreak, levelList));
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "SoulCrystalsTable: Could not parse soul_crystals.xml file: " + e.getMessage(), e);
		}
		
		_log.info("SoulCrystalsTable: Loaded " + _soulCrystals.size() + " SC data and " + _npcLevelingInfos.size() + " NPC data.");
	}
	
	public static final Map<Integer, SoulCrystalData> getSoulCrystalInfos()
	{
		return _soulCrystals;
	}
	
	public static final Map<Integer, LevelingInfo> getNpcInfos()
	{
		return _npcLevelingInfos;
	}
}