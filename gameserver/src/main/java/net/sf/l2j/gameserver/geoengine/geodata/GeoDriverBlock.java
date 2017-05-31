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

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import net.sf.l2j.Config;
import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.geoengine.geodata.blocks.Block;
import net.sf.l2j.gameserver.geoengine.geodata.blocks.ComplexBlock;
import net.sf.l2j.gameserver.geoengine.geodata.blocks.FlatBlock;
import net.sf.l2j.gameserver.geoengine.geodata.blocks.MultilayerBlock;
import net.sf.l2j.gameserver.geoengine.geodata.blocks.NullBlock;
import net.sf.l2j.gameserver.model.L2World;

/**
 * @author Hasha
 */
public final class GeoDriverBlock extends GeoData
{
	private final Block[][] _blocks;
	
	private final NullBlock _nullBlock;
	
	public GeoDriverBlock()
	{
		// load region files
		_blocks = new Block[GeoStructure.GEO_BLOCKS_X][GeoStructure.GEO_BLOCKS_Y];
		
		// prepare null block
		_nullBlock = new NullBlock();
		
		// initialize the bytebuffer for loading of multilayer blocks
		MultilayerBlock._byteBuffer = ByteBuffer.allocate(Short.MAX_VALUE);
		
		// load geo files according to geoengine config setup
		final ExProperties props = Config.load(Config.GEOENGINE_FILE);
		int loaded = 0;
		for (int rx = L2World.TILE_X_MIN; rx <= L2World.TILE_X_MAX; rx++)
		{
			for (int ry = L2World.TILE_Y_MIN; ry <= L2World.TILE_Y_MAX; ry++)
			{
				if (props.containsKey(String.valueOf(rx) + "_" + String.valueOf(ry)))
				{
					// region file is load-able, try to load it
					if (loadGeoBlocks(rx, ry))
						loaded++;
				}
				else
				{
					// region file is not load-able, load null blocks
					loadNullBlocks(rx, ry);
				}
			}
		}
		_log.info("GeoDriverBlock: Loaded " + loaded + " " + Config.GEODATA_FORMAT.toString() + " region files.");
		
		// release the bytebuffer for loading of multilayer blocks
		MultilayerBlock._byteBuffer = null;
	}
	
	private final boolean loadGeoBlocks(int regionX, int regionY)
	{
		final String filename = String.format(Config.GEODATA_FORMAT.getFilename(), regionX, regionY);
		
		// standard load
		try (RandomAccessFile raf = new RandomAccessFile(Config.GEODATA_PATH + filename, "r"); FileChannel fc = raf.getChannel())
		{
			// initialize file buffer
			MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).load();
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			// load 18B header for L2off geodata (1st and 2nd byte...region X and Y)
			if (Config.GEODATA_FORMAT == GeoFormat.L2OFF)
			{
				for (int i = 0; i < 18; i++)
					buffer.get();
			}
			
			// get block indexes
			final int blockX = (regionX - L2World.TILE_X_MIN) * GeoStructure.REGION_BLOCKS_X;
			final int blockY = (regionY - L2World.TILE_Y_MIN) * GeoStructure.REGION_BLOCKS_Y;
			
			// loop over region blocks
			for (int ix = 0; ix < GeoStructure.REGION_BLOCKS_X; ix++)
			{
				for (int iy = 0; iy < GeoStructure.REGION_BLOCKS_Y; iy++)
				{
					if (Config.GEODATA_FORMAT != GeoFormat.L2OFF)
					{
						// get block type
						final byte type = buffer.get();
						
						// load block according to block type
						switch (type)
						{
							case GeoStructure.TYPE_FLAT_L2J_L2OFF:
							case GeoStructure.TYPE_FLAT_L2D:
								_blocks[blockX + ix][blockY + iy] = new FlatBlock(buffer);
								break;
							
							case GeoStructure.TYPE_COMPLEX_L2J:
							case GeoStructure.TYPE_COMPLEX_L2D:
								_blocks[blockX + ix][blockY + iy] = new ComplexBlock(buffer);
								break;
							
							case GeoStructure.TYPE_MULTILAYER_L2J:
							case GeoStructure.TYPE_MULTILAYER_L2D:
								_blocks[blockX + ix][blockY + iy] = new MultilayerBlock(buffer);
								break;
							
							default:
								throw new IllegalArgumentException("Unknown block type: " + type);
						}
					}
					else
					{
						// get block type
						final short type = buffer.getShort();
						
						// load block according to block type
						switch (type)
						{
							case GeoStructure.TYPE_FLAT_L2J_L2OFF:
								_blocks[blockX + ix][blockY + iy] = new FlatBlock(buffer);
								break;
							
							case GeoStructure.TYPE_COMPLEX_L2OFF:
								_blocks[blockX + ix][blockY + iy] = new ComplexBlock(buffer);
								break;
							
							default:
								_blocks[blockX + ix][blockY + iy] = new MultilayerBlock(buffer);
								break;
						}
					}
				}
			}
			
			// check data consistency
			if (buffer.remaining() > 0)
				_log.warning("GeoDriverBlock: Region file " + filename + " can be corrupted, remaining " + buffer.remaining() + " bytes to read.");
			
			// loading was successful
			return true;
		}
		catch (Exception e)
		{
			// an error occured while loading, load null blocks
			_log.warning("GeoDriverBlock: Error while loading " + filename + " region file.");
			_log.warning(e.getMessage());
			
			// replace whole region file with null blocks
			loadNullBlocks(regionX, regionY);
			
			// loading was not successful
			return false;
		}
	}
	
	/**
	 * Loads null blocks. Used when no region file is detected or an error occurs during loading.
	 * @param regionX : First block X index.
	 * @param regionY : First block Y index.
	 */
	private final void loadNullBlocks(int regionX, int regionY)
	{
		// get block indexes
		final int blockX = (regionX - L2World.TILE_X_MIN) * GeoStructure.REGION_BLOCKS_X;
		final int blockY = (regionY - L2World.TILE_Y_MIN) * GeoStructure.REGION_BLOCKS_Y;
		
		// load all null blocks
		for (int ix = 0; ix < GeoStructure.REGION_BLOCKS_X; ix++)
		{
			for (int iy = 0; iy < GeoStructure.REGION_BLOCKS_Y; iy++)
			{
				_blocks[blockX + ix][blockY + iy] = _nullBlock;
			}
		}
	}
	
	@Override
	public int getGeoX(int worldX)
	{
		if (worldX < L2World.WORLD_X_MIN || worldX > L2World.WORLD_X_MAX)
			throw new IllegalArgumentException();
		
		return (worldX - L2World.WORLD_X_MIN) >> 4;
	}
	
	@Override
	public int getGeoY(int worldY)
	{
		if (worldY < L2World.WORLD_Y_MIN || worldY > L2World.WORLD_Y_MAX)
			throw new IllegalArgumentException();
		
		return (worldY - L2World.WORLD_Y_MIN) >> 4;
	}
	
	@Override
	public int getWorldX(int geoX)
	{
		if (geoX < 0 || geoX >= GeoStructure.GEO_CELLS_X)
			throw new IllegalArgumentException();
		
		return (geoX << 4) + L2World.WORLD_X_MIN + 8;
	}
	
	@Override
	public int getWorldY(int geoY)
	{
		if (geoY < 0 || geoY >= GeoStructure.GEO_CELLS_Y)
			throw new IllegalArgumentException();
		
		return (geoY << 4) + L2World.WORLD_Y_MIN + 8;
	}
	
	@Override
	public boolean hasGeoPos(int geoX, int geoY)
	{
		return getBlock(geoX, geoY).hasGeoPos();
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getHeightNearest(geoX, geoY, worldZ);
	}
	
	@Override
	public short getHeightAbove(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getHeightAbove(geoX, geoY, worldZ);
	}
	
	@Override
	public short getHeightBelow(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getHeightBelow(geoX, geoY, worldZ);
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getNsweNearest(geoX, geoY, worldZ);
	}
	
	public final Block getBlock(int geoX, int geoY)
	{
		return _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY / GeoStructure.BLOCK_CELLS_Y];
	}
}