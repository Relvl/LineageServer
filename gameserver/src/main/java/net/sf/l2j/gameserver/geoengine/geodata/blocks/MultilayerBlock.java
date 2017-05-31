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
import java.util.Arrays;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.geodata.GeoFormat;
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;

/**
 * @author Hasha
 */
public class MultilayerBlock extends Block
{
	public static ByteBuffer _byteBuffer;
	
	private final byte[] _buffer;
	
	/**
	 * Creates MultilayerBlock.
	 * @param bb : Input byte buffer.
	 */
	public MultilayerBlock(ByteBuffer bb)
	{
		// move buffer pointer to end of MultilayerBlock
		for (int cell = 0; cell < GeoStructure.BLOCK_CELLS; cell++)
		{
			// get layer count for this cell
			final byte layers = Config.GEODATA_FORMAT != GeoFormat.L2OFF ? bb.get() : (byte) bb.getShort();
			
			if (layers <= 0 || layers > Byte.MAX_VALUE)
				throw new RuntimeException("Invalid layer count for MultilayerBlock");
			
			// add layers count
			_byteBuffer.put(layers);
			
			// loop over layers
			for (byte layer = 0; layer < layers; layer++)
			{
				if (Config.GEODATA_FORMAT != GeoFormat.L2D)
				{
					// get data
					short data = bb.getShort();
					
					// get nswe
					_byteBuffer.put((byte) (data & 0x000F));
					
					// get height
					data = (short) ((short) (data & 0xFFF0) >> 1);
					_byteBuffer.put((byte) (data & 0x00FF));
					_byteBuffer.put((byte) (data >> 8));
				}
				else
				{
					// get nswe
					byte nswe = bb.get();
					_byteBuffer.put(nswe);
					
					// get height
					short height = bb.getShort();
					_byteBuffer.put((byte) (height & 0x00FF));
					_byteBuffer.put((byte) (height >> 8));
				}
			}
		}
		
		// initialize buffer
		_buffer = Arrays.copyOf(_byteBuffer.array(), _byteBuffer.position());
		
		// clear temp buffer
		_byteBuffer.clear();
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
		final int index = getCellIndexNearest(geoX, geoY, worldZ);
		
		// get height
		return (short) (_buffer[index + 1] & 0x00FF | _buffer[index + 2] << 8);
	}
	
	@Override
	public short getHeightAbove(int geoX, int geoY, int worldZ)
	{
		// move buffer index to cell
		int index = 0;
		for (int i = 0; i < (geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y); i++)
		{
			// move index by amount of layers for this cell
			index += _buffer[index] * 3 + 1;
		}
		
		// get layers count and shift to first layer data
		short layers = _buffer[index++];
		
		// loop though all cell layers, find closest layer above worldZ
		while (layers-- > 0)
		{
			// get layer height
			final short height = (short) (_buffer[index + 1] & 0x00FF | _buffer[index + 2] << 8);
			
			// layer height is higher than worldZ, return layer
			if (height > worldZ)
				return height;
			
			// move index to next layer
			index += 3;
		}
		
		// none layer found, throw exception
		throw new IndexOutOfBoundsException();
	}
	
	@Override
	public short getHeightBelow(int geoX, int geoY, int worldZ)
	{
		// move buffer index to cell
		int index = 0;
		for (int i = 0; i < (geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y); i++)
		{
			// move index by amount of layers for this cell
			index += _buffer[index] * 3 + 1;
		}
		
		// get layers count and shift to first layer data
		short layers = _buffer[index++];
		
		// set result to minimum possible value
		short result = Short.MIN_VALUE;
		
		// loop though all cell layers, find closest layer above worldZ
		while (layers-- > 0)
		{
			// get layer height
			final short height = (short) (_buffer[index + 1] & 0x00FF | _buffer[index + 2] << 8);
			
			// layer height is higher than worldZ, skip layer
			if (height >= worldZ)
				break;
			
			// layer height is higher than temporarily value, update value
			if (height > result)
				result = height;
			
			// move index to next layer
			index += 3;
		}
		
		// none layer found, throw exception
		if (result == Short.MIN_VALUE)
			throw new IndexOutOfBoundsException();
		
		// get height
		return result;
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		// get cell index
		final int index = getCellIndexNearest(geoX, geoY, worldZ);
		
		// get nswe
		return _buffer[index];
	}
	
	/**
	 * Returns cell data of the cell in closes layer to given coordinates.
	 * @param geoX : Geo X.
	 * @param geoY : Geo Y.
	 * @param worldZ : World Z.
	 * @return short : Cell index.
	 */
	private final int getCellIndexNearest(int geoX, int geoY, int worldZ)
	{
		// move buffer index to cell
		int index = 0;
		for (int i = 0; i < (geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y); i++)
		{
			// move index by amount of layers for this cell
			index += _buffer[index] * 3 + 1;
		}
		
		// get layers count
		short layers = _buffer[index++];
		
		// loop though all cell layers, find closest layer and return cell index
		int dZ = Integer.MAX_VALUE;
		while (layers-- > 0)
		{
			// get layer height
			final short tempHeight = (short) (_buffer[index + 1] & 0x00FF | _buffer[index + 2] << 8);
			
			// get Z distance
			final int tempDz = Math.abs(tempHeight - worldZ);
			
			// compare Z distances
			if (tempDz < dZ)
			{
				// update Z distance
				dZ = tempDz;
				
				// move index to next layer
				index += 3;
			}
			else
				break;
		}
		
		// return the layer, pointer to nswe
		return (short) (index - 3);
	}
}