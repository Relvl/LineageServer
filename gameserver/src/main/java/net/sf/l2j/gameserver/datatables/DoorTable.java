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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.template.CharTemplate;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DoorTable
{
	private static final Logger _log = Logger.getLogger(DoorTable.class.getName());
	
	private final Map<Integer, L2DoorInstance> _staticItems = new HashMap<>();
	private final Map<Integer, ArrayList<L2DoorInstance>> _regions = new HashMap<>();
	
	public static DoorTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected DoorTable()
	{
		parseData();
		onStart();
	}
	
	public void reload()
	{
		_staticItems.clear();
		_regions.clear();
		
		parseData();
	}
	
	public void parseData()
	{
		try
		{
			File f = new File("./data/xml/doors.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("door"))
						{
							// Initialize variables.
							int castleId = 0;
							// int sChId = 0;
							
							int x = 0;
							int y = 0;
							int z = 0;
							
							int rangeXMin = 0;
							int rangeYMin = 0;
							int rangeZMin = 0;
							
							int rangeXMax = 0;
							int rangeYMax = 0;
							int rangeZMax = 0;
							
							int hp = 0;
							int pdef = 0;
							int mdef = 0;
							
							boolean unlockable = false;
							int collisionRadius = 0;
							
							NamedNodeMap attrs = d.getAttributes();
							
							// Verify if the door got an id, else skip it
							Node att = attrs.getNamedItem("id");
							if (att == null)
							{
								_log.severe("DoorTable: Missing id for door, skipping.");
								continue;
							}
							int id = Integer.valueOf(att.getNodeValue());
							
							// Verify if the door got a name, else skip it
							att = attrs.getNamedItem("name");
							if (att == null)
							{
								_log.severe("DoorTable: Missing name for door id: " + id + ", skipping.");
								continue;
							}
							String name = att.getNodeValue();
							
							for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
							{
								attrs = c.getAttributes();
								if ("castle".equalsIgnoreCase(c.getNodeName()))
								{
									castleId = Integer.valueOf(attrs.getNamedItem("id").getNodeValue());
								}
								else if ("siegableclanhall".equalsIgnoreCase(c.getNodeName()))
								{
									// FIXME sChId = Integer.valueOf(attrs.getNamedItem("id").getNodeValue());
								}
								else if ("position".equalsIgnoreCase(c.getNodeName()))
								{
									x = Integer.valueOf(attrs.getNamedItem("x").getNodeValue());
									y = Integer.valueOf(attrs.getNamedItem("y").getNodeValue());
									z = Integer.valueOf(attrs.getNamedItem("z").getNodeValue());
								}
								else if ("minpos".equalsIgnoreCase(c.getNodeName()))
								{
									rangeXMin = Integer.valueOf(attrs.getNamedItem("x").getNodeValue());
									rangeYMin = Integer.valueOf(attrs.getNamedItem("y").getNodeValue());
									rangeZMin = Integer.valueOf(attrs.getNamedItem("z").getNodeValue());
								}
								else if ("maxpos".equalsIgnoreCase(c.getNodeName()))
								{
									rangeXMax = Integer.valueOf(attrs.getNamedItem("x").getNodeValue());
									rangeYMax = Integer.valueOf(attrs.getNamedItem("y").getNodeValue());
									rangeZMax = Integer.valueOf(attrs.getNamedItem("z").getNodeValue());
								}
								else if ("stats".equalsIgnoreCase(c.getNodeName()))
								{
									hp = Integer.valueOf(attrs.getNamedItem("hp").getNodeValue());
									pdef = Integer.valueOf(attrs.getNamedItem("pdef").getNodeValue());
									mdef = Integer.valueOf(attrs.getNamedItem("mdef").getNodeValue());
								}
								else if ("unlockable".equalsIgnoreCase(c.getNodeName()))
									unlockable = Boolean.valueOf(attrs.getNamedItem("val").getNodeValue());
							}
							
							if (rangeXMin > rangeXMax)
								_log.severe("DoorTable: Error on rangeX min/max, ID:" + id);
							if (rangeYMin > rangeYMax)
								_log.severe("DoorTable: Error on rangeY min/max, ID:" + id);
							if (rangeZMin > rangeZMax)
								_log.severe("DoorTable: Error on rangeZ min/max, ID:" + id);
							
							if ((rangeXMax - rangeXMin) > (rangeYMax - rangeYMin))
								collisionRadius = rangeYMax - rangeYMin;
							else
								collisionRadius = rangeXMax - rangeXMin;
							
							// Template initialization
							final StatsSet npcDat = new StatsSet();
							
							npcDat.set("id", id);
							npcDat.set("name", name);
							
							npcDat.set("hp", hp);
							npcDat.set("mp", 0);
							
							npcDat.set("hpRegen", 3.e-3f);
							npcDat.set("mpRegen", 3.e-3f);
							
							npcDat.set("radius", collisionRadius);
							npcDat.set("height", rangeZMax - rangeZMin);
							
							npcDat.set("pAtk", 0);
							npcDat.set("mAtk", 0);
							npcDat.set("pDef", pdef);
							npcDat.set("mDef", mdef);
							
							npcDat.set("runSpd", 0); // Have to keep this, static object MUST BE 0 (critical error otherwise).
							
							final L2DoorInstance door = new L2DoorInstance(IdFactory.getInstance().getNextId(), new CharTemplate(npcDat), id, name, unlockable);
							door.setRange(rangeXMin, rangeYMin, rangeZMin, rangeXMax, rangeYMax, rangeZMax);
							door.setCurrentHpMp(door.getMaxHp(), door.getMaxMp());
							door.setXYZInvisible(x, y, z);
							door.setMapRegion(MapRegionTable.getMapRegion(x, y));
							door.setOpen(false);
							
							// Attach door to a castle if a castleId is found
							if (castleId > 0)
							{
								Castle castle = CastleManager.getInstance().getCastleById(castleId);
								if (castle != null)
								{
									// Set the door as a wall if door name contains "wall".
									if (name.contains("wall"))
										door.setIsWall(true);
									
									castle.getDoors().add(door); // Add the door to castle doors list.
									
									if (Config.DEBUG)
										_log.warning("DoorTable: Door " + door.getDoorId() + " is now attached to " + castle.getName() + " castle.");
								}
							}
							// Test door, and attach it to a CH if a CH is found near
							else
							{
								ClanHall clanhall = ClanHallManager.getInstance().getNearbyClanHall(door.getX(), door.getY(), 500);
								if (clanhall != null)
								{
									clanhall.getDoors().add(door); // Add the door to CH doors list.
									door.setClanHall(clanhall);
									
									if (Config.DEBUG)
										_log.warning("DoorTable: Door " + door.getDoorId() + " is now attached to " + clanhall.getName() + " clanhall.");
								}
							}
							
							_staticItems.put(door.getDoorId(), door);
							
							if (_regions.containsKey(door.getMapRegion()))
								_regions.get(door.getMapRegion()).add(door);
							else
							{
								final ArrayList<L2DoorInstance> region = new ArrayList<>();
								region.add(door);
								
								_regions.put(door.getMapRegion(), region);
							}
							
							door.spawnMe(door.getX(), door.getY(), door.getZ());
						}
					}
				}
			}
			
			_log.info("DoorTable: Loaded " + _staticItems.size() + " doors templates for " + _regions.size() + " regions.");
		}
		catch (Exception e)
		{
			_log.warning("DoorTable: Error while creating table: " + e);
		}
	}
	
	public L2DoorInstance getDoor(Integer id)
	{
		return _staticItems.get(id);
	}
	
	public Collection<L2DoorInstance> getDoors()
	{
		return _staticItems.values();
	}
	
	public boolean checkIfDoorsBetween(Location start, Location end)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
	}
	
	public boolean checkIfDoorsBetween(int ox, int oy, int oz, int tx, int ty, int tz)
	{
		List<L2DoorInstance> doors = _regions.get(MapRegionTable.getMapRegion(ox, oy));
		if (doors == null)
			return false;
		
		for (L2DoorInstance door : doors)
		{
			if (door.isOpened() || door.getCurrentHp() <= 0)
				continue;
			
			int maxX = door.getXMax();
			int maxY = door.getYMax();
			int maxZ = door.getZMax();
			int minX = door.getXMin();
			int minY = door.getYMin();
			int minZ = door.getZMin();
			
			// line segment goes through box
			// first basic checks to stop most calculations short
			// phase 1, x
			if ((ox <= maxX && tx >= minX) || (tx <= maxX && ox >= minX))
			{
				// phase 2, y
				if ((oy <= maxY && ty >= minY) || (ty <= maxY && oy >= minY))
				{
					// phase 3, basically only z remains but now we calculate it with another formula (by rage)
					// in some cases the direct line check (only) in the beginning isn't sufficient,
					// when char z changes a lot along the path
					int l = tx - ox;
					int m = ty - oy;
					int n = tz - oz;
					
					int dk;
					
					if ((dk = (door.getA() * l + door.getB() * m + door.getC() * n)) == 0)
						continue; // Parallel
						
					float p = (float) (door.getA() * ox + door.getB() * oy + door.getC() * oz + door.getD()) / (float) dk;
					
					int fx = (int) (ox - l * p);
					int fy = (int) (oy - m * p);
					int fz = (int) (oz - n * p);
					
					if ((Math.min(ox, tx) <= fx && fx <= Math.max(ox, tx)) && (Math.min(oy, ty) <= fy && fy <= Math.max(oy, ty)) && (Math.min(oz, tz) <= fz && fz <= Math.max(oz, tz)))
					{
						if (((fx >= minX && fx <= maxX) || (fx >= maxX && fx <= minX)) && ((fy >= minY && fy <= maxY) || (fy >= maxY && fy <= minY)) && ((fz >= minZ && fz <= maxZ) || (fz >= maxZ && fz <= minZ)))
							return true; // Door between
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Simple operations to handle at server startup :
	 * <ul>
	 * <li>Open some doors types.</li>
	 * <li>Schedule open/close tasks.</li>
	 * <li>Load castle doors upgrades.</li>
	 * </ul>
	 */
	private void onStart()
	{
		try
		{
			// Open following doors at server start: coliseums, ToI - RB Area Doors
			getDoor(24190001).openMe();
			getDoor(24190002).openMe();
			getDoor(24190003).openMe();
			getDoor(24190004).openMe();
			getDoor(23180001).openMe();
			getDoor(23180002).openMe();
			getDoor(23180003).openMe();
			getDoor(23180004).openMe();
			getDoor(23180005).openMe();
			getDoor(23180006).openMe();
			
			// Schedules a task to automatically open/close doors
			for (L2DoorInstance doorInst : getDoors())
			{
				// Garden of Eva (every 7 minutes)
				if (doorInst.getName().startsWith("Eva"))
					doorInst.setAutoActionDelay(420000);
				// Tower of Insolence (every 5 minutes)
				else if (doorInst.getName().startsWith("hubris"))
					doorInst.setAutoActionDelay(300000);
			}
			
			// Load doors upgrades.
			for (Castle castle : CastleManager.getInstance().getCastles())
				castle.loadDoorUpgrade();
		}
		catch (NullPointerException e)
		{
			_log.log(Level.WARNING, "There are errors in doors.xml.", e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final DoorTable _instance = new DoorTable();
	}
}