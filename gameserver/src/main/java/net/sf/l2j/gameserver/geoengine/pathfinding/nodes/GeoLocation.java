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
package net.sf.l2j.gameserver.geoengine.pathfinding.nodes;

import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.model.Location;

/**
 * @author Hasha
 */
public class GeoLocation extends Location
{
	private byte _nswe;
	
	public GeoLocation(int x, int y, int z)
	{
		super(x, y, GeoData.getInstance().getHeightNearest(x, y, z));
		_nswe = GeoData.getInstance().getNsweNearest(x, y, z);
	}
	
	public void set(int x, int y, short z)
	{
		super.setXYZ(x, y, GeoData.getInstance().getHeightNearest(x, y, z));
		_nswe = GeoData.getInstance().getNsweNearest(x, y, z);
	}
	
	public int getGeoX()
	{
		return _x;
	}
	
	public int getGeoY()
	{
		return _y;
	}
	
	@Override
	public int getX()
	{
		return GeoData.getInstance().getWorldX(_x);
	}
	
	@Override
	public int getY()
	{
		return GeoData.getInstance().getWorldY(_y);
	}
	
	public byte getNSWE()
	{
		return _nswe;
	}
}