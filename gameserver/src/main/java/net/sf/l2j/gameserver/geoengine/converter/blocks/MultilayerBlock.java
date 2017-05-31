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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.geodata.GeoFormat;
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;

/**
 * @author Hasha
 */
public final class MultilayerBlock extends Block
{
	private final MultilayerCell[] _cells;
	
	/**
	 * Creates MultilayerBlock.
	 * @param bb : Input byte buffer.
	 */
	public MultilayerBlock(ByteBuffer bb)
	{
		_cells = new MultilayerCell[GeoStructure.BLOCK_CELLS];
		
		// move buffer pointer to end of MultilayerBlock
		for (int index = 0; index < GeoStructure.BLOCK_CELLS; index++)
		{
			// get layer count for this cell
			final byte layers = Config.GEODATA_FORMAT == GeoFormat.L2J ? bb.get() : (byte) bb.getShort();
			
			if (layers <= 0 || layers > Byte.MAX_VALUE)
				throw new RuntimeException("Invalid layer count for MultilayerBlock");
			
			MultilayerCell cell = new MultilayerCell(layers);
			
			// loop over layers
			for (byte layer = 0; layer < layers; layer++)
			{
				// get data
				short data = bb.getShort();
				
				// set data
				cell.setData(layer, (byte) (data & 0x000F), (short) ((short) (data & 0xFFF0) >> 1));
			}
			
			cell.sort();
			
			_cells[index] = cell;
		}
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		// get cell
		final MultilayerCell cell = _cells[(geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)];
		
		// loop though all cell layers, find closest layer and return cell index
		int dZ = Integer.MAX_VALUE;
		for (int i = 0; i < cell.getLayers(); i++)
		{
			// get layer height
			final short tempHeight = cell.getHeight(i);
			
			// get Z distance
			final int tempDz = Math.abs(tempHeight - worldZ);
			
			// compare Z distances
			if (tempDz < dZ)
			{
				// update Z distance
				dZ = tempDz;
			}
			else
				return cell.getHeight(--i);
		}
		
		// return the layer, pointer to nswe
		return cell.getHeight(0);
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		// get cell
		final MultilayerCell cell = _cells[(geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)];
		
		// loop though all cell layers, find closest layer below worldZ
		int limit = Integer.MAX_VALUE;
		int index = 0;
		for (; index < cell.getLayers(); index++)
		{
			// get layer height
			final short height = cell.getHeight(index);
			
			// get distance and compare with limit
			final int distance = Math.abs(height - worldZ);
			if (distance >= limit)
				break;
			
			// update distance
			limit = distance;
		}
		
		// return layer nswe
		return cell.getNSWE(--index);
	}
	
	@Override
	public byte getNsweBelow(int geoX, int geoY, int worldZ)
	{
		// get cell
		final MultilayerCell cell = _cells[(geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)];
		
		// loop though all cell layers, find closest layer below worldZ
		int index = -1;
		for (int i = 0; i < cell.getLayers(); i++)
		{
			// get layer height and compare height with worldZ
			final short height = cell.getHeight(i);
			if (height >= worldZ)
				break;
			
			// set index
			index = i;
		}
		
		// none layer found, return 0 (no movement)
		if (index < 0)
			return 0;
		
		// return layer nswe
		return cell.getNSWE(index);
	}
	
	@Override
	public void saveBlock(BufferedOutputStream stream) throws IOException
	{
		stream.write(GeoStructure.TYPE_MULTILAYER_L2D);
		
		for (MultilayerCell cell : _cells)
		{
			int layers = cell.getLayers();
			stream.write((byte) layers);
			
			for (int layer = 0; layer < layers; layer++)
			{
				stream.write(cell.getNSWE(layer));
				
				short height = cell.getHeight(layer);
				stream.write((byte) (height & 0x00FF));
				stream.write((byte) (height >> 8));
			}
		}
	}
	
	public final MultilayerCell getGeoCells(int geoX, int geoY)
	{
		return _cells[(geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)];
	}
}