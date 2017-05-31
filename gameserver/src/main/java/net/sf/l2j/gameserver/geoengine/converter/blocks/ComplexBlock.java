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
package net.sf.l2j.gameserver.geoengine.converter.blocks;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;

/**
 * @author Hasha
 */
public final class ComplexBlock extends Block
{
	private final byte[] _nswe;
	private final short[] _height;
	
	/**
	 * Creates ComplexBlock.
	 * @param bb : Input byte buffer.
	 */
	public ComplexBlock(ByteBuffer bb)
	{
		// initialize buffer (short height + byte nswe)
		_nswe = new byte[GeoStructure.BLOCK_CELLS];
		_height = new short[GeoStructure.BLOCK_CELLS];
		
		// load data
		for (int i = 0; i < GeoStructure.BLOCK_CELLS; i++)
		{
			// get data
			short data = bb.getShort();
			
			// get nswe
			_nswe[i] = (byte) (data & 0x000F);
			
			// get height
			_height[i] = (short) ((short) (data & 0xFFF0) >> 1);
		}
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		// get cell index
		final int index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y));
		
		// get height
		return _height[index];
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		// get cell index
		final int index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y));
		
		// get nswe
		return _nswe[index];
	}
	
	@Override
	public byte getNsweBelow(int geoX, int geoY, int worldZ)
	{
		// get cell index
		final int index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y));
		
		// get nswe
		return _nswe[index];
	}
	
	@Override
	public void saveBlock(BufferedOutputStream stream) throws IOException
	{
		stream.write(GeoStructure.TYPE_COMPLEX_L2D);
		
		for (int i = 0; i < GeoStructure.BLOCK_CELLS; i++)
		{
			stream.write(_nswe[i]);
			
			stream.write((byte) (_height[i] & 0x00FF));
			stream.write((byte) (_height[i] >> 8));
		}
	}
	
	public void updateNSWE(int geoX, int geoY, byte nswe)
	{
		// get cell index
		final int index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y));
		
		// get nswe
		_nswe[index] = nswe;
	}
}
