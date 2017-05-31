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
package net.sf.l2j.gameserver.geoengine.geodata.blocks;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.geodata.GeoFormat;

/**
 * @author Hasha
 */
public class NullBlock extends Block
{
	private byte _nswe;
	
	public NullBlock()
	{
		_nswe = Config.GEODATA_FORMAT != GeoFormat.L2D ? 0x0F : (byte) 0xFF;
	}
	
	@Override
	public boolean hasGeoPos()
	{
		return false;
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		return (short) worldZ;
	}
	
	@Override
	public short getHeightAbove(int geoX, int geoY, int worldZ)
	{
		return (short) worldZ;
	}
	
	@Override
	public short getHeightBelow(int geoX, int geoY, int worldZ)
	{
		return (short) worldZ;
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		return _nswe;
	}
}