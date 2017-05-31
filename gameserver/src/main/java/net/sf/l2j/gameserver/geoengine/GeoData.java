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
package net.sf.l2j.gameserver.geoengine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.geodata.GeoDriverArray;
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;
import net.sf.l2j.gameserver.geoengine.geodata.NullDriver;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;

/**
 * @author Hasha
 */
public abstract class GeoData
{
	protected static final Logger _log = Logger.getLogger(GeoData.class.getName());
	
	private static GeoData _driver;
	
	private static BufferedOutputStream _geoBugs;
	
	/**
	 * Initializes geodata.
	 */
	public static final void initialize()
	{
		_log.log(Level.INFO, "GeoData: Initializing...");
		
		// load geodata driver
		if (Config.GEODATA <= 0)
			_driver = new NullDriver();
		else
			_driver = new GeoDriverArray();
		// _driver = new GeoDriverBlock();
		
		// initialize bug reports
		try
		{
			_geoBugs = new BufferedOutputStream(new FileOutputStream(new File(Config.GEODATA_PATH + "geo_bugs.txt"), true));
		}
		catch (Exception e)
		{
			_log.warning("GeoDriverArray: Could not load \"geo_bugs.txt\" file.");
		}
	}
	
	public static final GeoData getInstance()
	{
		return _driver;
	}
	
	/**
	 * Converts world X to geodata X.
	 * @param worldX
	 * @return int : Geo X
	 */
	public abstract int getGeoX(int worldX);
	
	/**
	 * Converts world Y to geodata Y.
	 * @param worldY
	 * @return int : Geo Y
	 */
	public abstract int getGeoY(int worldY);
	
	/**
	 * Converts geodata X to world X.
	 * @param geoX
	 * @return int : World X
	 */
	public abstract int getWorldX(int geoX);
	
	/**
	 * Converts geodata Y to world Y.
	 * @param geoY
	 * @return int : World Y
	 */
	public abstract int getWorldY(int geoY);
	
	/**
	 * Check if geo coordinates has geo.
	 * @param geoX : Geodata X
	 * @param geoY : Geodata Y
	 * @return boolean : True, if given geo coordinates have geodata
	 */
	public abstract boolean hasGeoPos(int geoX, int geoY);
	
	/**
	 * Returns the height of cell, which is closest to given coordinates.
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell geodata Z coordinate, closest to given coordinates.
	 */
	public abstract short getHeightNearest(int geoX, int geoY, int worldZ);
	
	/**
	 * Returns the height of cell, which is first above given coordinates.
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell geodata Z coordinate, above given coordinates.
	 */
	public abstract short getHeightAbove(int geoX, int geoY, int worldZ);
	
	/**
	 * Returns the height of cell, which is first below given coordinates.
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell geodata Z coordinate, below given coordinates.
	 */
	public abstract short getHeightBelow(int geoX, int geoY, int worldZ);
	
	/**
	 * Returns the NSWE flag byte of cell, which is closes to given coordinates.
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell NSWE flag byte coordinate, closest to given coordinates.
	 */
	public abstract byte getNsweNearest(int geoX, int geoY, int worldZ);
	
	/**
	 * Record a geodata bug.
	 * @param loc : Location of the geodata bug.
	 * @param comment : Short commentary.
	 * @return boolean : True, when bug was successfully recorded.
	 */
	public final boolean addGeoBug(Location loc, String comment)
	{
		int gox = getGeoX(loc.getX());
		int goy = getGeoY(loc.getY());
		int rx = gox / GeoStructure.REGION_CELLS_X + L2World.TILE_X_MIN;
		int ry = goy / GeoStructure.REGION_CELLS_Y + L2World.TILE_Y_MIN;
		int bx = (gox / GeoStructure.BLOCK_CELLS_X) % GeoStructure.REGION_BLOCKS_X;
		int by = (goy / GeoStructure.BLOCK_CELLS_Y) % GeoStructure.REGION_BLOCKS_Y;
		int cx = gox % GeoStructure.BLOCK_CELLS_X;
		int cy = goy % GeoStructure.BLOCK_CELLS_Y;
		
		String out = rx + ";" + ry + ";" + bx + ";" + by + ";" + cx + ";" + cy + ";" + loc.getZ() + ";" + comment.replace(";", ":") + "\n";
		try
		{
			_geoBugs.write(out.getBytes());
			_geoBugs.flush();
			return true;
		}
		catch (Exception e)
		{
			_log.warning("GeoData: Could not save new entry to \"geo_bugs.txt\" file.");
			return false;
		}
	}
	
	/**
	 * Check if world coordinates has geo.
	 * @param worldX : World X
	 * @param worldY : World Y
	 * @return boolean : True, if given world coordinates have geodata
	 */
	public boolean hasGeo(int worldX, int worldY)
	{
		return hasGeoPos(getGeoX(worldX), getGeoY(worldY));
	}
	
	/**
	 * Returns closest Z coordinate according to geodata.
	 * @param worldX : world x
	 * @param worldY : world y
	 * @param worldZ : world z
	 * @return short : nearest Z coordinates according to geodata
	 */
	public short getHeight(int worldX, int worldY, int worldZ)
	{
		return getHeightNearest(getGeoX(worldX), getGeoY(worldY), worldZ);
	}
}