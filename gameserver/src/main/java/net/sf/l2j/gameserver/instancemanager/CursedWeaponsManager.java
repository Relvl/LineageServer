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
package net.sf.l2j.gameserver.instancemanager;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2FeedableBeastInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FestivalMonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RiftInvaderInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;
import net.sf.l2j.gameserver.model.entity.CursedWeapon;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Manager for CWs.
 * <ul>
 * <li><u>dropRate :</u> the drop rate used to monster drop the CW. Default : 1/1000000</li>
 * <li><u>duration :</u> the overall lifetime duration in hours. Default : 72 hours (3 days)</li>
 * <li><u>durationLost :</u> the task time duration, launched when someone pickups a CW. Renewed when CW owner kills a player. Default : 24 hours.</li>
 * <li><u>disapearChance :</u> chance to dissapear when CW owner dies. Default : 50%</li>
 * <li><u>stageKills :</u> the basic number used to calculate random needed number of needed kills to rank up the CW. That number is used as a base, it takes a random number between 50% and 150% of that value. Default : 10</li>
 * </ul>
 * @author Micht, Tryskell
 */
public class CursedWeaponsManager
{
	private static final Logger _log = Logger.getLogger(CursedWeaponsManager.class.getName());
	
	private final Map<Integer, CursedWeapon> _cursedWeapons = new HashMap<>();
	
	public static final CursedWeaponsManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public CursedWeaponsManager()
	{
		load();
	}
	
	public void reload()
	{
		// Drop existing CWs.
		for (CursedWeapon cw : _cursedWeapons.values())
			cw.endOfLife();
		
		_cursedWeapons.clear();
		load();
	}
	
	private void load()
	{
		if (!Config.ALLOW_CURSED_WEAPONS)
		{
			_log.info("CursedWeaponsManager: Skipping loading.");
			return;
		}
		
		try
		{
			File file = new File("./data/xml/cursed_weapons.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(file);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if ("item".equalsIgnoreCase(d.getNodeName()))
				{
					NamedNodeMap attrs = d.getAttributes();
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
					String name = attrs.getNamedItem("name").getNodeValue();
					
					CursedWeapon cw = new CursedWeapon(id, skillId, name);
					
					int val;
					for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
					{
						if ("dropRate".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
							cw.setDropRate(val);
						}
						else if ("duration".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
							cw.setDuration(val);
						}
						else if ("durationLost".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
							cw.setDurationLost(val);
						}
						else if ("disapearChance".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
							cw.setDisapearChance(val);
						}
						else if ("stageKills".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
							cw.setStageKills(val);
						}
					}
					
					// load data from SQL
					cw.loadData();
					
					// Store cursed weapon
					_cursedWeapons.put(id, cw);
				}
			}
			
			_log.info("CursedWeaponsManager: Loaded " + _cursedWeapons.size() + " cursed weapons.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error parsing cursed_weapons.xml: ", e);
		}
	}
	
	/**
	 * Checks if a CW can drop. Verify if CW is already active, or if the L2Attackable you killed was a good type.
	 * @param attackable : The target to test.
	 * @param player : The killer of the L2Attackable.
	 */
	public synchronized void checkDrop(L2Attackable attackable, L2PcInstance player)
	{
		if (attackable instanceof L2SiegeGuardInstance || attackable instanceof L2RiftInvaderInstance || attackable instanceof L2FestivalMonsterInstance || attackable instanceof L2GrandBossInstance || attackable instanceof L2FeedableBeastInstance)
			return;
		
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActive())
				continue;
			
			if (cw.checkDrop(attackable, player))
				break;
		}
	}
	
	/**
	 * Assimilate a weapon if you already possess one (and rank up possessed weapon), or activate it otherwise.
	 * @param player : The player to test.
	 * @param item : The item player picked up.
	 */
	public void activate(L2PcInstance player, ItemInstance item)
	{
		final CursedWeapon cw = _cursedWeapons.get(item.getItemId());
		if (player.isCursedWeaponEquipped()) // cannot own 2 cursed swords
		{
			_cursedWeapons.get(player.getCursedWeaponEquippedId()).rankUp();
			
			// Setup the player in order to drop the weapon from inventory.
			cw.setPlayer(player);
			
			// erase the newly obtained cursed weapon
			cw.endOfLife();
		}
		else
			cw.activate(player, item);
	}
	
	public void drop(int itemId, L2Character killer)
	{
		_cursedWeapons.get(itemId).dropIt(killer);
	}
	
	public void increaseKills(int itemId)
	{
		_cursedWeapons.get(itemId).increaseKills();
	}
	
	public int getCurrentStage(int itemId)
	{
		return _cursedWeapons.get(itemId).getCurrentStage();
	}
	
	/**
	 * This method is used on EnterWorld in order to check if the player is equipped with a CW.
	 * @param player
	 */
	public void checkPlayer(L2PcInstance player)
	{
		if (player == null)
			return;
		
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActivated() && player.getObjectId() == cw.getPlayerId())
			{
				cw.setPlayer(player);
				cw.setItem(player.getInventory().getItemByItemId(cw.getItemId()));
				cw.giveDemonicSkills();
				player.setCursedWeaponEquippedId(cw.getItemId());
			}
		}
	}
	
	public boolean isCursed(int itemId)
	{
		return _cursedWeapons.containsKey(itemId);
	}
	
	public Collection<CursedWeapon> getCursedWeapons()
	{
		return _cursedWeapons.values();
	}
	
	public Set<Integer> getCursedWeaponsIds()
	{
		return _cursedWeapons.keySet();
	}
	
	public CursedWeapon getCursedWeapon(int itemId)
	{
		return _cursedWeapons.get(itemId);
	}
	
	private static class SingletonHolder
	{
		protected static final CursedWeaponsManager _instance = new CursedWeaponsManager();
	}
}