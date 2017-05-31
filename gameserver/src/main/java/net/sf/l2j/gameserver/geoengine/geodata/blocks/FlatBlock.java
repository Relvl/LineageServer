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

import java.nio.ByteBuffer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.geodata.GeoFormat;

/**
 * @author Hasha
 */
public class FlatBlock extends Block
{
	private final short _height;
	private byte _nswe;
	
	/**
	 * Creates FlatBlock.
	 * @param bb : Input byte buffer.
	 */
	public FlatBlock(ByteBuffer bb)
	{
		_height = bb.getShort();
		_nswe = Config.GEODATA_FORMAT != GeoFormat.L2D ? 0x0F : (byte) (0xFF);
		
		if (Config.GEODATA_FORMAT == GeoFormat.L2OFF)
			bb.getShort();
	}
	
	@Override
	public boolean hasGeoPos()
	{
		return true;
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		return _height;
	}
	
	@Override
	public short getHeightAbove(int geoX, int geoY, int worldZ)
	{
		return _height;
	}
	
	@Override
	public short getHeightBelow(int geoX, int geoY, int worldZ)
	{
		return _height;
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		return _nswe;
	}
}