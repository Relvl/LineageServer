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
package net.sf.l2j.gameserver.geoengine.geodata;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.geoengine.GeoData;

/**
 * @author Hasha
 */
public final class NullDriver extends GeoData
{
	private static final Logger _log = Logger.getLogger(NullDriver.class.getName());
	
	public NullDriver()
	{
		_log.log(Level.INFO, "NullDriver: Ready.");
	}
	
	@Override
	public int getGeoX(int worldX)
	{
		return worldX;
	}
	
	@Override
	public int getGeoY(int worldY)
	{
		return worldY;
	}
	
	@Override
	public int getWorldX(int geoX)
	{
		return geoX;
	}
	
	@Override
	public int getWorldY(int geoY)
	{
		return geoY;
	}
	
	@Override
	public boolean hasGeoPos(int geoX, int geoY)
	{
		return false;
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		return (short) worldZ;
	}
	
	@Override
	public final short getHeightAbove(int geoX, int geoY, int worldZ)
	{
		return (short) worldZ;
	}
	
	@Override
	public final short getHeightBelow(int geoX, int geoY, int worldZ)
	{
		return (short) worldZ;
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		return 0x0F;
	}
}