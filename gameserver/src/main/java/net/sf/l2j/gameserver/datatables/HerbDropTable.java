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
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class loads herbs drop rules.
 */
public class HerbDropTable
{
	private static Logger _log = Logger.getLogger(HerbDropTable.class.getName());
	
	private final Map<Integer, List<DropCategory>> _herbGroups = new HashMap<>();
	
	public static HerbDropTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected HerbDropTable()
	{
		try
		{
			File file = new File("./data/xml/herbs_droplist.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(file);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if ("group".equalsIgnoreCase(d.getNodeName()))
				{
					NamedNodeMap attrs = d.getAttributes();
					int groupId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					
					List<DropCategory> category;
					if (_herbGroups.containsKey(groupId))
						category = _herbGroups.get(groupId);
					else
					{
						category = new ArrayList<>();
						_herbGroups.put(groupId, category);
					}
					
					for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
					{
						DropData dropDat = new DropData();
						if ("item".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							int categoryType = Integer.parseInt(attrs.getNamedItem("category").getNodeValue());
							int chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
							
							dropDat.setItemId(id);
							dropDat.setMinDrop(1);
							dropDat.setMaxDrop(1);
							dropDat.setChance(chance);
							
							if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
							{
								_log.warning("HerbDropTable: Herb data for undefined item template! GroupId: " + groupId + ", itemId: " + dropDat.getItemId());
								continue;
							}
							
							boolean catExists = false;
							for (DropCategory cat : category)
							{
								// if the category exists, add the drop to this category.
								if (cat.getCategoryType() == categoryType)
								{
									cat.addDropData(dropDat, false);
									catExists = true;
									break;
								}
							}
							
							// if the category doesn't exit, create it and add the drop
							if (!catExists)
							{
								DropCategory cat = new DropCategory(categoryType);
								cat.addDropData(dropDat, false);
								category.add(cat);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("HerbDropTable: Error while creating table: " + e);
		}
		_log.info("HerbDropTable: Loaded " + _herbGroups.size() + " herbs groups.");
	}
	
	public List<DropCategory> getHerbDroplist(int groupId)
	{
		return _herbGroups.get(groupId);
	}
	
	private static class SingletonHolder
	{
		protected static final HerbDropTable _instance = new HerbDropTable();
	}
}