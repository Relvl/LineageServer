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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class TeleportLocationTable
{
	private static final Logger _log = Logger.getLogger(TeleportLocationTable.class.getName());
	
	private static final Map<Integer, L2TeleportLocation> _teleports = new HashMap<>();
	
	public static TeleportLocationTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected TeleportLocationTable()
	{
		load();
	}
	
	public void reload()
	{
		_teleports.clear();
		load();
	}
	
	public void load()
	{
		try
		{
			File f = new File("./data/xml/teleports.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("teleport"))
				{
					NamedNodeMap node = d.getAttributes();
					
					L2TeleportLocation teleport = new L2TeleportLocation();
					teleport.setTeleId(Integer.valueOf(node.getNamedItem("id").getNodeValue()));
					teleport.setLocX(Integer.valueOf(node.getNamedItem("loc_x").getNodeValue()));
					teleport.setLocY(Integer.valueOf(node.getNamedItem("loc_y").getNodeValue()));
					teleport.setLocZ(Integer.valueOf(node.getNamedItem("loc_z").getNodeValue()));
					teleport.setPrice(Integer.valueOf(node.getNamedItem("price").getNodeValue()));
					teleport.setIsForNoble(Integer.valueOf(node.getNamedItem("fornoble").getNodeValue()) == 1);
					
					_teleports.put(teleport.getTeleId(), teleport);
				}
			}
		}
		catch (Exception e)
		{
			_log.severe("TeleportLocationTable: Error while creating table" + e);
		}
		_log.info("TeleportLocationTable: Loaded " + _teleports.size() + " templates.");
	}
	
	/**
	 * @param id The id to retrieve.
	 * @return the _teleports table line using that id.
	 */
	public L2TeleportLocation getTemplate(int id)
	{
		return _teleports.get(id);
	}
	
	private static class SingletonHolder
	{
		protected static final TeleportLocationTable _instance = new TeleportLocationTable();
	}
}