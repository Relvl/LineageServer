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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2AccessLevel;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author FBIagent
 */
public class AccessLevels
{
	private static Logger _log = Logger.getLogger(AccessLevels.class.getName());
	
	public static final int MASTER_ACCESS_LEVEL_NUMBER = Config.MASTERACCESS_LEVEL;
	public static final int USER_ACCESS_LEVEL_NUMBER = 0;
	
	public static L2AccessLevel MASTER_ACCESS_LEVEL = new L2AccessLevel(MASTER_ACCESS_LEVEL_NUMBER, "Master Access", Config.MASTERACCESS_NAME_COLOR, Config.MASTERACCESS_TITLE_COLOR, null, true, true, true, true, true, true, true, true);
	public static L2AccessLevel USER_ACCESS_LEVEL = new L2AccessLevel(USER_ACCESS_LEVEL_NUMBER, "User", 0xFFFFFF, 0xFFFF77, null, false, false, false, true, false, true, true, true);
	
	private final Map<Integer, L2AccessLevel> _accessLevels = new HashMap<>();
	
	public static AccessLevels getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected AccessLevels()
	{
		try
		{
			File f = new File("./data/xml/access_levels.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("access"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					int accessLevel = Integer.valueOf(attrs.getNamedItem("level").getNodeValue());
					String name = attrs.getNamedItem("name").getNodeValue();
					
					if (accessLevel == USER_ACCESS_LEVEL_NUMBER)
					{
						_log.log(Level.WARNING, "AccessLevels: Access level " + name + " is using reserved user access level " + USER_ACCESS_LEVEL_NUMBER + ". Ignoring it!");
						continue;
					}
					else if (accessLevel == MASTER_ACCESS_LEVEL_NUMBER)
					{
						_log.log(Level.WARNING, "AccessLevels: Access level " + name + " is using reserved master access level " + MASTER_ACCESS_LEVEL_NUMBER + ". Ignoring it!");
						continue;
					}
					else if (accessLevel < 0)
					{
						_log.log(Level.WARNING, "AccessLevels: Access level " + name + " is using banned access level (below 0). Ignoring it!");
						continue;
					}
					
					int nameColor;
					try
					{
						nameColor = Integer.decode("0x" + attrs.getNamedItem("nameColor").getNodeValue());
					}
					catch (NumberFormatException nfe)
					{
						nameColor = Integer.decode("0xFFFFFF");
					}
					
					int titleColor;
					try
					{
						titleColor = Integer.decode("0x" + attrs.getNamedItem("titleColor").getNodeValue());
					}
					catch (NumberFormatException nfe)
					{
						titleColor = Integer.decode("0x77FFFF");
					}
					
					String childs = attrs.getNamedItem("childAccess").getNodeValue();
					boolean isGm = Boolean.valueOf(attrs.getNamedItem("isGm").getNodeValue());
					boolean allowPeaceAttack = Boolean.valueOf(attrs.getNamedItem("allowPeaceAttack").getNodeValue());
					boolean allowFixedRes = Boolean.valueOf(attrs.getNamedItem("allowFixedRes").getNodeValue());
					boolean allowTransaction = Boolean.valueOf(attrs.getNamedItem("allowTransaction").getNodeValue());
					boolean allowAltG = Boolean.valueOf(attrs.getNamedItem("allowAltg").getNodeValue());
					boolean giveDamage = Boolean.valueOf(attrs.getNamedItem("giveDamage").getNodeValue());
					boolean takeAggro = Boolean.valueOf(attrs.getNamedItem("takeAggro").getNodeValue());
					boolean gainExp = Boolean.valueOf(attrs.getNamedItem("gainExp").getNodeValue());
					
					_accessLevels.put(accessLevel, new L2AccessLevel(accessLevel, name, nameColor, titleColor, childs.isEmpty() ? null : childs, isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp));
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "AccessLevels: Error loading from database: " + e.getMessage(), e);
		}
		
		_log.info("AccessLevels: Loaded " + _accessLevels.size() + " accesses.");
		
		// Add finally the normal user access level.
		_accessLevels.put(USER_ACCESS_LEVEL_NUMBER, USER_ACCESS_LEVEL);
	}
	
	/**
	 * Returns the access level by characterAccessLevel
	 * @param accessLevelNum as int
	 * @return AccessLevel: AccessLevel instance by char access level<br>
	 */
	public L2AccessLevel getAccessLevel(int accessLevelNum)
	{
		L2AccessLevel accessLevel = null;
		
		synchronized (_accessLevels)
		{
			accessLevel = _accessLevels.get(accessLevelNum);
		}
		return accessLevel;
	}
	
	public void addBanAccessLevel(int accessLevel)
	{
		synchronized (_accessLevels)
		{
			if (accessLevel > -1)
				return;
			
			_accessLevels.put(accessLevel, new L2AccessLevel(accessLevel, "Banned", -1, -1, null, false, false, false, false, false, false, false, false));
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AccessLevels _instance = new AccessLevels();
	}
}