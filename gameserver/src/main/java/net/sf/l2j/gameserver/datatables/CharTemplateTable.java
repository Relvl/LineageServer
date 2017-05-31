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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.actor.template.PcTemplate;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author Unknown, Forsaiken
 */
public class CharTemplateTable
{
	private static final Logger _log = Logger.getLogger(CharTemplateTable.class.getName());
	
	private final Map<Integer, PcTemplate> _templates = new HashMap<>();
	
	public static CharTemplateTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected CharTemplateTable()
	{
		final File mainDir = new File("./data/xml/classes");
		if (!mainDir.isDirectory())
		{
			_log.log(Level.SEVERE, "CharTemplateTable: Main dir " + mainDir.getAbsolutePath() + " hasn't been found.");
			return;
		}
		
		for (final File file : mainDir.listFiles())
		{
			if (file.isFile() && file.getName().endsWith(".xml"))
				loadFileClass(file);
		}
		
		_log.log(Level.INFO, "CharTemplateTable: Loaded " + _templates.size() + " character templates.");
		_log.log(Level.INFO, "CharTemplateTable: Loaded " + SkillTreeTable.getInstance().getSkillTreesSize() + " classes skills trees.");
	}
	
	private void loadFileClass(final File f)
	{
		try
		{
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if ("class".equalsIgnoreCase(d.getNodeName()))
				{
					NamedNodeMap attrs = d.getAttributes();
					StatsSet set = new StatsSet();
					
					final int classId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					final int parentId = Integer.parseInt(attrs.getNamedItem("parentId").getNodeValue());
					String items = null;
					
					set.set("classId", classId);
					
					for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
					{
						if ("set".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							String name = attrs.getNamedItem("name").getNodeValue().trim();
							String value = attrs.getNamedItem("val").getNodeValue().trim();
							set.set(name, value);
						}
						else if ("skillTrees".equalsIgnoreCase(cd.getNodeName()))
						{
							List<L2SkillLearn> skills = new ArrayList<>();
							for (Node cb = cd.getFirstChild(); cb != null; cb = cb.getNextSibling())
							{
								L2SkillLearn skillLearn = null;
								if ("skill".equalsIgnoreCase(cb.getNodeName()))
								{
									attrs = cb.getAttributes();
									final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
									final int lvl = Integer.parseInt(attrs.getNamedItem("lvl").getNodeValue());
									final int minLvl = Integer.parseInt(attrs.getNamedItem("minLvl").getNodeValue());
									final int cost = Integer.parseInt(attrs.getNamedItem("sp").getNodeValue());
									skillLearn = new L2SkillLearn(id, lvl, minLvl, cost, 0, 0);
									skills.add(skillLearn);
								}
							}
							SkillTreeTable.getInstance().addSkillsToSkillTrees(skills, classId, parentId);
						}
						else if ("items".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							items = attrs.getNamedItem("val").getNodeValue().trim();
						}
					}
					PcTemplate pcT = new PcTemplate(set);
					
					// Add items listed in "items" if class possess a filled "items" string.
					if (items != null)
					{
						String[] itemsSplit = items.split(";");
						for (String element : itemsSplit)
							pcT.addItem(Integer.parseInt(element));
					}
					
					_templates.put(pcT.getClassId().getId(), pcT);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "CharTemplateTable: Error loading from file: " + f.getName(), e);
		}
	}
	
	public PcTemplate getTemplate(ClassId classId)
	{
		return _templates.get(classId.getId());
	}
	
	public PcTemplate getTemplate(int classId)
	{
		return _templates.get(classId);
	}
	
	public final String getClassNameById(int classId)
	{
		PcTemplate pcTemplate = _templates.get(classId);
		if (pcTemplate == null)
			throw new IllegalArgumentException("No template for classId: " + classId);
		
		return pcTemplate.getClassName();
	}
	
	private static class SingletonHolder
	{
		protected static final CharTemplateTable _instance = new CharTemplateTable();
	}
}