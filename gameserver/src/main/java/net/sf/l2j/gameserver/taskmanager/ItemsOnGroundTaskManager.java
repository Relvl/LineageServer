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
package net.sf.l2j.gameserver.taskmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;

/**
 * Destroys item on ground after specified time. When server is about to shutdown/restart, saves all dropped items in to SQL. Loads them during server start.
 * @author Hasha
 */
public final class ItemsOnGroundTaskManager implements Runnable
{
	private static final Logger _log = Logger.getLogger(ItemsOnGroundTaskManager.class.getName());
	
	private static final String LOAD_ITEMS = "SELECT object_id,item_id,count,enchant_level,x,y,z,time FROM items_on_ground";
	private static final String DELETE_ITEMS = "DELETE FROM items_on_ground";
	private static final String SAVE_ITEMS = "INSERT INTO items_on_ground(object_id,item_id,count,enchant_level,x,y,z,time) VALUES(?,?,?,?,?,?,?,?)";
	
	private final Map<L2ItemInstance, Long> _items = new ConcurrentHashMap<>();
	
	public static final ItemsOnGroundTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public ItemsOnGroundTaskManager()
	{
		// Run task each 5 seconds.
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, 5000, 5000);
		
		// Item saving is disabled, return.
		if (!Config.SAVE_DROPPED_ITEM)
			return;
		
		// Load all items.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Get current time.
			final long time = System.currentTimeMillis();
			
			ResultSet result = con.createStatement().executeQuery(LOAD_ITEMS);
			while (result.next())
			{
				// TODO: maybe add destroy item check here and remove mercenary ticket handling system
				
				// Create new item.
				final L2ItemInstance item = new L2ItemInstance(result.getInt(1), result.getInt(2));
				L2World.getInstance().addObject(item);
				
				// Check and set count.
				final int count = result.getInt(3);
				if (item.isStackable() && count > 1)
					item.setCount(count);
				
				// Check and set enchant.
				final int enchant = result.getInt(4);
				if (enchant > 0)
					item.setEnchantLevel(enchant);
				
				// Spawn item in the world.
				item.getPosition().setWorldPosition(result.getInt(5), result.getInt(6), result.getInt(7));
				L2WorldRegion region = L2World.getInstance().getRegion(item.getPosition().getWorldPosition());
				item.getPosition().setWorldRegion(region);
				region.addVisibleObject(item);
				item.setIsVisible(true);
				L2World.getInstance().addVisibleObject(item, item.getPosition().getWorldRegion());
				
				// Get interval, add item to the list.
				long interval = result.getLong(8);
				if (interval == 0)
					_items.put(item, (long) 0);
				else
					_items.put(item, time + interval);
			}
			result.close();
			
			_log.info("ItemsOnGroundTaskManager: Restored " + _items.size() + " items on ground.");
		}
		catch (Exception e)
		{
			_log.warning("ItemsOnGroundTaskManager: Error while loading \"items_on_ground\" table: " + e.getMessage());
		}
		
		// Delete all items from database.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE_ITEMS);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warning("ItemsOnGroundTaskManager: Can not empty \"items_on_ground\" table to save new items: " + e.getMessage());
		}
	}
	
	/**
	 * Adds {@link L2ItemInstance} to the ItemAutoDestroyTask.
	 * @param item : {@link L2ItemInstance} to be added and checked.
	 * @param actor : {@link L2Character} who dropped the item.
	 */
	public final void add(L2ItemInstance item, L2Character actor)
	{
		// Actor doesn't exist or item is protected, do not store item to destroy task (e.g. tickets for castle mercenaries -> handled by other manager)
		if (actor == null || item.isDestroyProtected())
			return;
		
		long dropTime = 0;
		
		// Item has special destroy time, use it.
		Integer special = Config.SPECIAL_ITEM_DESTROY_TIME.get(item.getItemId());
		if (special != null)
			dropTime = special;
		// Get base destroy time for herbs, items, equipable items.
		else if (item.isHerb())
			dropTime = Config.HERB_AUTO_DESTROY_TIME;
		else if (item.isEquipable())
			dropTime = Config.EQUIPABLE_ITEM_AUTO_DESTROY_TIME;
		else
			dropTime = Config.ITEM_AUTO_DESTROY_TIME;
		
		// Item was dropped by playable, apply the multiplier.
		if (actor instanceof L2Playable)
			dropTime *= Config.PLAYER_DROPPED_ITEM_MULTIPLIER;
		
		// If drop time exists, set real drop time.
		if (dropTime != 0)
			dropTime += System.currentTimeMillis();
		
		// Put item to drop list.
		_items.put(item, dropTime);
	}
	
	/**
	 * Removes {@link L2ItemInstance} from the ItemAutoDestroyTask.
	 * @param item : {@link L2ItemInstance} to be removed.
	 */
	public final void remove(L2ItemInstance item)
	{
		_items.remove(item);
	}
	
	@Override
	public final void run()
	{
		// List is empty, skip.
		if (_items.isEmpty())
			return;
		
		// Get current time.
		final long time = System.currentTimeMillis();
		
		// Loop all items.
		for (Entry<L2ItemInstance, Long> entry : _items.entrySet())
		{
			// Get and validate destroy time.
			final long destroyTime = entry.getValue();
			
			// Item can't be destroyed, skip.
			if (destroyTime == 0)
				continue;
			
			// Time hasn't passed yet, skip.
			if (time < destroyTime)
				continue;
			
			// Destroy item and remove from task.
			final L2ItemInstance item = entry.getKey();
			L2World.getInstance().removeVisibleObject(item, item.getWorldRegion());
			L2World.getInstance().removeObject(item);
			_items.remove(item);
		}
	}
	
	public final void save()
	{
		// Item saving is disabled, return.
		if (!Config.SAVE_DROPPED_ITEM)
		{
			_log.info("ItemsOnGroundTaskManager: Item save is disabled.");
			return;
		}
		
		// List is empty, return.
		if (_items.isEmpty())
		{
			_log.info("ItemsOnGroundTaskManager: List is empty.");
			return;
		}
		
		// Store whole items list to database.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Get current time.
			final long time = System.currentTimeMillis();
			
			PreparedStatement statement = con.prepareStatement(SAVE_ITEMS);
			for (Entry<L2ItemInstance, Long> entry : _items.entrySet())
			{
				// Get item and destroy time interval.
				final L2ItemInstance item = entry.getKey();
				
				// Cursed Items not saved to ground, prevent double save.
				if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
					continue;
				
				try
				{
					statement.setInt(1, item.getObjectId());
					statement.setInt(2, item.getItemId());
					statement.setInt(3, item.getCount());
					statement.setInt(4, item.getEnchantLevel());
					statement.setInt(5, item.getX());
					statement.setInt(6, item.getY());
					statement.setInt(7, item.getZ());
					long left = entry.getValue();
					if (left == 0)
						statement.setLong(8, 0);
					else
						statement.setLong(8, left - time);
					
					statement.execute();
					statement.clearParameters();
				}
				catch (Exception e)
				{
					_log.warning("ItemsOnGroundTaskManager: Error while saving item id=" + item.getItemId() + " name=" + item.getName() + ": " + e.getMessage());
				}
			}
			statement.close();
			
			_log.info("ItemsOnGroundTaskManager: Saved " + _items.size() + " items on ground.");
		}
		catch (SQLException e)
		{
			_log.warning("ItemsOnGroundTaskManager: Could not save items on ground to \"items_on_ground\" table: " + e.getMessage());
		}
	}
	
	private static class SingletonHolder
	{
		protected static final ItemsOnGroundTaskManager _instance = new ItemsOnGroundTaskManager();
	}
}