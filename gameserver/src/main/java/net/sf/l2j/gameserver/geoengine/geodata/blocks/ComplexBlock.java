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
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;

/**
 * @author Hasha
 */
public final class ComplexBlock extends Block
{
	private final byte[] _buffer;
	
	/**
	 * Creates ComplexBlock.
	 * @param bb : Input byte buffer.
	 */
	public ComplexBlock(ByteBuffer bb)
	{
		// initialize buffer
		_buffer = new byte[GeoStructure.BLOCK_CELLS * 3];
		
		// load data
		for (int i = 0; i < GeoStructure.BLOCK_CELLS; i++)
		{
			if (Config.GEODATA_FORMAT != GeoFormat.L2D)
			{
				// get data
				short data = bb.getShort();
				
				// get nswe
				_buffer[i * 3] = (byte) (data & 0x000F);
				
				// get height
				data = (short) ((short) (data & 0xFFF0) >> 1);
				_buffer[i * 3 + 1] = (byte) (data & 0x00FF);
				_buffer[i * 3 + 2] = (byte) (data >> 8);
			}
			else
			{
				// get nswe
				_buffer[i * 3] = bb.get();
				
				// get height
				short height = bb.getShort();
				_buffer[i * 3 + 1] = (byte) (height & 0x00FF);
				_buffer[i * 3 + 2] = (byte) (height >> 8);
			}
		}
	}
	
	@Override
	public boolean hasGeoPos()
	{
		return true;
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		// get cell index
		final int index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)) * 3;
		
		// get height
		return (short) (_buffer[index + 1] & 0x00FF | _buffer[index + 2] << 8);
	}
	
	@Override
	public short getHeightAbove(int geoX, int geoY, int worldZ)
	{
		// get cell index
		final int index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)) * 3;
		
		// get height
		return (short) (_buffer[index + 1] & 0x00FF | _buffer[index + 2] << 8);
	}
	
	@Override
	public short getHeightBelow(int geoX, int geoY, int worldZ)
	{
		// get cell index
		final int index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)) * 3;
		
		// get height
		return (short) (_buffer[index + 1] & 0x00FF | _buffer[index + 2] << 8);
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		// get cell index
		final int index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)) * 3;
		
		// get nswe
		return _buffer[index];
	}
}