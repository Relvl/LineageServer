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

/**
 * @author FBIagent
 */
package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.item.SummonItem;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SummonItemsData
{
	private static final Logger _log = Logger.getLogger(SummonItemsData.class.getName());
	
	private static final Map<Integer, SummonItem> _summonitems = new HashMap<>();
	
	public static SummonItemsData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected SummonItemsData()
	{
		try
		{
			File f = new File("./data/xml/summon_items.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("summon_item"))
				{
					NamedNodeMap node = d.getAttributes();
					
					int itemID = Integer.valueOf(node.getNamedItem("itemID").getNodeValue());
					int npcID = Integer.valueOf(node.getNamedItem("npcID").getNodeValue());
					byte summonType = Byte.valueOf(node.getNamedItem("summonType").getNodeValue());
					
					_summonitems.put(itemID, new SummonItem(itemID, npcID, summonType));
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("SummonItemsData: Error while creating SummonItemsData table: " + e);
		}
		_log.info("SummonItemsData: Loaded " + _summonitems.size() + " templates.");
	}
	
	public SummonItem getSummonItem(int itemId)
	{
		return _summonitems.get(itemId);
	}
	
	private static class SingletonHolder
	{
		protected static final SummonItemsData _instance = new SummonItemsData();
	}
}