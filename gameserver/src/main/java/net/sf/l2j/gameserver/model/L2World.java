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
package net.sf.l2j.gameserver.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;

public final class L2World
{
	private static Logger _log = Logger.getLogger(L2World.class.getName());
	
	// Geodata min/max tiles
	public static final int TILE_X_MIN = 16;
	public static final int TILE_X_MAX = 26;
	public static final int TILE_Y_MIN = 10;
	public static final int TILE_Y_MAX = 25;
	
	// Map dimensions
	public static final int TILE_SIZE = 32768;
	public static final int WORLD_X_MIN = (TILE_X_MIN - 20) * TILE_SIZE;
	public static final int WORLD_X_MAX = (TILE_X_MAX - 19) * TILE_SIZE;
	public static final int WORLD_Y_MIN = (TILE_Y_MIN - 18) * TILE_SIZE;
	public static final int WORLD_Y_MAX = (TILE_Y_MAX - 17) * TILE_SIZE;
	
	// Regions and offsets
	private static final int REGION_SIZE = 4096;
	private static final int REGIONS_X = (WORLD_X_MAX - WORLD_X_MIN) / REGION_SIZE;
	private static final int REGIONS_Y = (WORLD_Y_MAX - WORLD_Y_MIN) / REGION_SIZE;
	private static final int REGION_X_OFFSET = Math.abs(WORLD_X_MIN / REGION_SIZE);
	private static final int REGION_Y_OFFSET = Math.abs(WORLD_Y_MIN / REGION_SIZE);
	
	private final Map<Integer, L2Object> _objects = new ConcurrentHashMap<>();
	private final Map<Integer, L2PetInstance> _pets = new ConcurrentHashMap<>();
	private final Map<Integer, L2PcInstance> _players = new ConcurrentHashMap<>();
	
	private final L2WorldRegion[][] _worldRegions = new L2WorldRegion[REGIONS_X + 1][REGIONS_Y + 1];
	
	protected L2World()
	{
		for (int i = 0; i <= REGIONS_X; i++)
		{
			for (int j = 0; j <= REGIONS_Y; j++)
				_worldRegions[i][j] = new L2WorldRegion(i, j);
		}
		
		for (int x = 0; x <= REGIONS_X; x++)
		{
			for (int y = 0; y <= REGIONS_Y; y++)
			{
				for (int a = -1; a <= 1; a++)
				{
					for (int b = -1; b <= 1; b++)
					{
						if (validRegion(x + a, y + b))
							_worldRegions[x + a][y + b].addSurroundingRegion(_worldRegions[x][y]);
					}
				}
			}
		}
		_log.info("L2World: WorldRegion grid (" + REGIONS_X + " by " + REGIONS_Y + ") is now setted up.");
	}
	
	public void addObject(L2Object object)
	{
		_objects.putIfAbsent(object.getObjectId(), object);
	}
	
	public void removeObject(L2Object object)
	{
		_objects.remove(object.getObjectId());
	}
	
	public Collection<L2Object> getObjects()
	{
		return _objects.values();
	}
	
	public L2Object getObject(int objectId)
	{
		return _objects.get(objectId);
	}
	
	public void addPlayer(L2PcInstance cha)
	{
		_players.putIfAbsent(cha.getObjectId(), cha);
	}
	
	public void removePlayer(L2PcInstance cha)
	{
		_players.remove(cha.getObjectId());
	}
	
	public Collection<L2PcInstance> getPlayers()
	{
		return _players.values();
	}
	
	public L2PcInstance getPlayer(String name)
	{
		return getPlayer(CharNameTable.getInstance().getIdByName(name));
	}
	
	public L2PcInstance getPlayer(int objectId)
	{
		return _players.get(objectId);
	}
	
	public L2PetInstance addPet(int ownerId, L2PetInstance pet)
	{
		return _pets.putIfAbsent(ownerId, pet);
	}
	
	public void removePet(int ownerId)
	{
		_pets.remove(ownerId);
	}
	
	public L2PetInstance getPet(int ownerId)
	{
		return _pets.get(ownerId);
	}
	
	/**
	 * Add a L2Object in the world.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Object (including L2PcInstance) are identified in <B>_visibleObjects</B> of his current L2WorldRegion and in <B>_knownObjects</B> of other surrounding L2Characters <BR>
	 * L2PcInstance are identified in <B>_allPlayers</B> of L2World, in <B>_allPlayers</B> of his current L2WorldRegion and in <B>_knownPlayer</B> of other surrounding L2Characters <BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Add the L2Object object in _allPlayers* of L2World</li> <li>Add the L2Object object in _gmList** of GmListTable</li> <li>Add object in _knownObjects and _knownPlayer* of all surrounding L2WorldRegion L2Characters</li><BR>
	 * <li>If object is a L2Character, add all surrounding L2Object in its _knownObjects and all surrounding L2PcInstance in its _knownPlayer</li><BR>
	 * <I>* only if object is a L2PcInstance</I><BR>
	 * <I>** only if object is a GM L2PcInstance</I><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object in _visibleObjects and _allPlayers* of L2WorldRegion (need synchronisation)</B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects and _allPlayers* of L2World (need synchronisation)</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Drop an Item</li> <li>Spawn a L2Character</li> <li>Apply Death Penalty of a L2PcInstance</li><BR>
	 * <BR>
	 * @param object L2object to add in the world
	 * @param newRegion L2WorldRegion in wich the object will be add (not used)
	 */
	public void addVisibleObject(L2Object object, L2WorldRegion newRegion)
	{
		if (object instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) object;
			
			if (!player.isTeleporting())
			{
				L2PcInstance tmp = _players.get(player.getObjectId());
				if (tmp != null)
				{
					_log.warning("Duplicate character!? Closing both characters (" + player.getName() + ")");
					player.logout();
					tmp.logout();
					return;
				}
				_players.putIfAbsent(player.getObjectId(), player);
			}
		}
		
		if (!newRegion.isActive())
			return;
		
		final boolean objectHasKnownlist = (object.getKnownList() != null);
		
		// tell the player about the surroundings
		// Go through the visible objects contained in the circular area
		for (L2Object visible : getVisibleObjects(object, 2000))
		{
			// Add the object in L2ObjectHashSet(L2Object) _knownObjects of the visible L2Character according to conditions :
			// - L2Character is visible
			// - object is not already known
			// - object is in the watch distance
			// If L2Object is a L2PcInstance, add L2Object in L2ObjectHashSet(L2PcInstance) _knownPlayer of the visible L2Character
			if (visible.getKnownList() != null)
				visible.getKnownList().addKnownObject(object);
			
			// Add the visible L2Object in L2ObjectHashSet(L2Object) _knownObjects of the object according to conditions
			// If visible L2Object is a L2PcInstance, add visible L2Object in L2ObjectHashSet(L2PcInstance) _knownPlayer of the object
			if (objectHasKnownlist)
				object.getKnownList().addKnownObject(visible);
		}
	}
	
	/**
	 * Remove a L2Object from the world.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Object (including L2PcInstance) are identified in <B>_visibleObjects</B> of his current L2WorldRegion and in <B>_knownObjects</B> of other surrounding L2Characters <BR>
	 * L2PcInstance are identified in <B>_allPlayers</B> of L2World, in <B>_allPlayers</B> of his current L2WorldRegion and in <B>_knownPlayer</B> of other surrounding L2Characters <BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2Object object from _allPlayers* of L2World</li> <li>Remove the L2Object object from _visibleObjects and _allPlayers* of L2WorldRegion</li> <li>Remove the L2Object object from _gmList** of GmListTable</li> <li>Remove object from _knownObjects and _knownPlayer* of all
	 * surrounding L2WorldRegion L2Characters</li><BR>
	 * <li>If object is a L2Character, remove all L2Object from its _knownObjects and all L2PcInstance from its _knownPlayer</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World</B></FONT><BR>
	 * <BR>
	 * <I>* only if object is a L2PcInstance</I><BR>
	 * <I>** only if object is a GM L2PcInstance</I><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Pickup an Item</li> <li>Decay a L2Character</li><BR>
	 * <BR>
	 * @param object L2object to remove from the world
	 * @param oldRegion L2WorldRegion in wich the object was before removing
	 */
	public void removeVisibleObject(L2Object object, L2WorldRegion oldRegion)
	{
		if (object == null)
			return;
		
		if (oldRegion != null)
		{
			// Remove the object from the L2ObjectHashSet(L2Object) _visibleObjects of L2WorldRegion
			// If object is a L2PcInstance, remove it from the L2ObjectHashSet(L2PcInstance) _allPlayers of this L2WorldRegion
			oldRegion.removeVisibleObject(object);
			
			final boolean objectHasKnownlist = (object.getKnownList() != null);
			
			// Go through all surrounding L2WorldRegion L2Characters
			for (L2WorldRegion reg : oldRegion.getSurroundingRegions())
			{
				for (L2Object obj : reg.getVisibleObjects().values())
				{
					if (obj.getKnownList() != null)
						obj.getKnownList().removeKnownObject(object);
					
					if (objectHasKnownlist)
						object.getKnownList().removeKnownObject(obj);
				}
			}
			
			// If object is a L2Character :
			// Remove all L2Object from L2ObjectHashSet(L2Object) containing all L2Object detected by the L2Character
			// Remove all L2PcInstance from L2ObjectHashSet(L2PcInstance) containing all player ingame detected by the L2Character
			if (objectHasKnownlist)
				object.getKnownList().removeAllKnownObjects();
			
			// If selected L2Object is a L2PcIntance, remove it from L2ObjectHashSet(L2PcInstance) _allPlayers of L2World
			if (object instanceof L2PcInstance)
			{
				if (!((L2PcInstance) object).isTeleporting())
					removePlayer((L2PcInstance) object);
			}
		}
	}
	
	/**
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All visible object are identified in <B>_visibleObjects</B> of their current L2WorldRegion <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in order to scan a large area around a L2Object
	 * @param object L2object that determine the center of the circular area
	 * @param radius Radius of the circular area
	 * @return all visible objects of the L2WorldRegions in the circular area (radius) centered on the object.
	 */
	public static List<L2Object> getVisibleObjects(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
			return new ArrayList<>();
		
		int x = object.getX();
		int y = object.getY();
		int sqRadius = radius * radius;
		
		// Create an FastList in order to contain all visible L2Object
		List<L2Object> result = new ArrayList<>();
		
		// Go through the FastList of region
		for (L2WorldRegion regi : object.getWorldRegion().getSurroundingRegions())
		{
			// Go through visible objects of the selected region
			for (L2Object _object : regi.getVisibleObjects().values())
			{
				if (_object == null || _object.equals(object))
					continue; // skip our own character
					
				int x1 = _object.getX();
				int y1 = _object.getY();
				
				double dx = x1 - x;
				double dy = y1 - y;
				
				// If the visible object is inside the circular area add the object to the FastList result
				if (dx * dx + dy * dy < sqRadius)
					result.add(_object);
			}
		}
		
		return result;
	}
	
	public static int getRegionX(int regionX)
	{
		return (regionX - REGION_X_OFFSET) * REGION_SIZE;
	}
	
	public static int getRegionY(int regionY)
	{
		return (regionY - REGION_Y_OFFSET) * REGION_SIZE;
	}
	
	/**
	 * @param point position of the object.
	 * @return the current L2WorldRegion of the object according to its position (x,y).
	 */
	public L2WorldRegion getRegion(Location point)
	{
		return getRegion(point.getX(), point.getY());
	}
	
	public L2WorldRegion getRegion(int x, int y)
	{
		return _worldRegions[(x - WORLD_X_MIN) / REGION_SIZE][(y - WORLD_Y_MIN) / REGION_SIZE];
	}
	
	/**
	 * @return the whole 2d array containing the world regions used by ZoneData.java to setup zones inside the world regions
	 */
	public L2WorldRegion[][] getWorldRegions()
	{
		return _worldRegions;
	}
	
	/**
	 * Check if the current L2WorldRegions of the object is valid according to its position (x,y).
	 * @param x X position of the object
	 * @param y Y position of the object
	 * @return True if the L2WorldRegion is valid
	 */
	private static boolean validRegion(int x, int y)
	{
		return (x >= 0 && x <= REGIONS_X && y >= 0 && y <= REGIONS_Y);
	}
	
	/**
	 * Deleted all spawns in the world.
	 */
	public void deleteVisibleNpcSpawns()
	{
		_log.info("Deleting all visible NPCs.");
		for (int i = 0; i <= REGIONS_X; i++)
		{
			for (int j = 0; j <= REGIONS_Y; j++)
				_worldRegions[i][j].deleteVisibleNpcSpawns();
		}
		_log.info("All visibles NPCs are now deleted.");
	}
	
	public static L2World getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final L2World _instance = new L2World();
	}
}