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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2AccessLevel;
import net.sf.l2j.gameserver.model.L2AdminCommandAccessRight;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author FBIagent
 */
public class AdminCommandAccessRights
{
	private static Logger _log = Logger.getLogger(AdminCommandAccessRights.class.getName());
	
	private final Map<String, L2AdminCommandAccessRight> _adminCommandAccessRights;
	
	public static AdminCommandAccessRights getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected AdminCommandAccessRights()
	{
		_adminCommandAccessRights = new HashMap<>();
		load();
	}
	
	public void reload()
	{
		_adminCommandAccessRights.clear();
		load();
	}
	
	private void load()
	{
		try
		{
			File f = new File("./data/xml/admin_commands_rights.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("aCar"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					String adminCommand = attrs.getNamedItem("name").getNodeValue();
					String accessLevels = attrs.getNamedItem("accessLevel").getNodeValue();
					_adminCommandAccessRights.put(adminCommand, new L2AdminCommandAccessRight(adminCommand, accessLevels));
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "AdminCommandAccessRights: Error loading from database:" + e.getMessage(), e);
		}
		
		_log.info("AdminCommandAccessRights: Loaded " + _adminCommandAccessRights.size() + " rights.");
	}
	
	public boolean hasAccess(String adminCommand, L2AccessLevel accessLevel)
	{
		if (accessLevel.getLevel() == AccessLevels.MASTER_ACCESS_LEVEL_NUMBER)
			return true;
		
		L2AdminCommandAccessRight acar = _adminCommandAccessRights.get(adminCommand);
		if (acar == null)
		{
			_log.info("AdminCommandAccessRights: No rights defined for admin command " + adminCommand + ".");
			return false;
		}
		
		return acar.hasAccess(accessLevel);
	}
	
	private static class SingletonHolder
	{
		protected static final AdminCommandAccessRights _instance = new AdminCommandAccessRights();
	}
}