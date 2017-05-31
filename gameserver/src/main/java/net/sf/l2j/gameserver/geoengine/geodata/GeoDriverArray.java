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
import java.util.Arrays;

import net.sf.l2j.Config;
import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.model.L2World;

/**
 * @author Hasha
 */
public final class GeoDriverArray extends GeoData
{
	private final byte[][][] _blocks;
	
	private final byte[] _nullBlock;
	
	private final byte _nswe;
	
	private static ByteBuffer _list = ByteBuffer.allocate(Short.MAX_VALUE);
	
	public GeoDriverArray()
	{
		// load region files
		_blocks = new byte[GeoStructure.GEO_BLOCKS_X][GeoStructure.GEO_BLOCKS_Y][];
		
		// prepare null block
		_nullBlock = new byte[]
		{
			GeoStructure.NULL
		};
		
		// default null and flat block NSWE
		_nswe = Config.GEODATA_FORMAT != GeoFormat.L2D ? 0x0F : (byte) 0xFF;
		
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
		_log.info("GeoDriverArray: Loaded " + loaded + " " + Config.GEODATA_FORMAT.toString() + " region files.");
		
		// release the bytebuffer for loading of multilayer blocks
		_list = null;
	}
	
	/**
	 * Loads geodata from a file. When diagonal strategy is enabled, precalculates diagonal flags for whole file. When file does not exist, is corrupted or not consistent, loads none geodata.
	 * @param regionX : Geodata file region X coordinate.
	 * @param regionY : Geodata file region Y coordinate.
	 * @return boolean : True, when geodata file was loaded without problem.
	 */
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
								_blocks[blockX + ix][blockY + iy] = loadFlatBlock(buffer);
								break;
							
							case GeoStructure.TYPE_COMPLEX_L2J:
							case GeoStructure.TYPE_COMPLEX_L2D:
								_blocks[blockX + ix][blockY + iy] = loadComplexBlock(buffer);
								break;
							
							case GeoStructure.TYPE_MULTILAYER_L2J:
							case GeoStructure.TYPE_MULTILAYER_L2D:
								_blocks[blockX + ix][blockY + iy] = loadMultilayerBlock(buffer);
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
								_blocks[blockX + ix][blockY + iy] = loadFlatBlock(buffer);
								break;
							
							case GeoStructure.TYPE_COMPLEX_L2OFF:
								_blocks[blockX + ix][blockY + iy] = loadComplexBlock(buffer);
								break;
							
							default:
								_blocks[blockX + ix][blockY + iy] = loadMultilayerBlock(buffer);
								break;
						}
					}
				}
			}
			
			// check data consistency
			if (buffer.remaining() > 0)
				_log.warning("GeoDriverArray: Region file " + filename + " can be corrupted, remaining " + buffer.remaining() + " bytes to read.");
			
			// loading was successful
			return true;
		}
		catch (Exception e)
		{
			// an error occured while loading, load null blocks
			_log.warning("GeoDriverArray: Error while loading " + filename + " region file.");
			_log.warning(e.getMessage());
			
			// replace whole region file with null blocks
			loadNullBlocks(regionX, regionY);
			
			// loading was not successful
			return false;
		}
	}
	
	/**
	 * Creates FlatBlock.
	 * @param bb : Input byte buffer.
	 * @return byte[] : Flat block in byte array.
	 */
	private static final byte[] loadFlatBlock(ByteBuffer bb)
	{
		// get height
		short height = bb.getShort();
		
		// read dummy byte for L2OFF geodata
		if (Config.GEODATA_FORMAT == GeoFormat.L2OFF)
			bb.getShort();
		
		// initialize buffer
		byte[] buffer = new byte[3];
		
		buffer[0] = GeoStructure.FLAT;
		buffer[1] = (byte) (height & 0x00FF);
		buffer[2] = (byte) (height >> 8);
		
		return buffer;
	}
	
	/**
	 * Creates ComplexBlock.
	 * @param bb : Input byte buffer.
	 * @return byte[] : Complex block in byte array.
	 */
	private static final byte[] loadComplexBlock(ByteBuffer bb)
	{
		// initialize buffer
		byte[] buffer = new byte[GeoStructure.BLOCK_CELLS * 3 + 1];
		
		buffer[0] = GeoStructure.COMPLEX;
		
		// load data
		for (int i = 0; i < GeoStructure.BLOCK_CELLS; i++)
		{
			// depending on geodata format
			if (Config.GEODATA_FORMAT != GeoFormat.L2D)
			{
				// get data
				short data = bb.getShort();
				
				// get nswe
				buffer[i * 3 + 1] = (byte) (data & 0x000F);
				
				// get height
				data = (short) ((short) (data & 0xFFF0) >> 1);
				buffer[i * 3 + 2] = (byte) (data & 0x00FF);
				buffer[i * 3 + 3] = (byte) (data >> 8);
			}
			else
			{
				// get nswe
				buffer[i * 3 + 1] = bb.get();
				
				// get height
				short height = bb.getShort();
				buffer[i * 3 + 2] = (byte) (height & 0x00FF);
				buffer[i * 3 + 3] = (byte) (height >> 8);
			}
		}
		
		return buffer;
	}
	
	/**
	 * Creates MultilayerBlock.
	 * @param bb : Input byte buffer.
	 * @return byte[] : Multilayer block in byte array.
	 */
	private static final byte[] loadMultilayerBlock(ByteBuffer bb)
	{
		_list.put(GeoStructure.MULTILAYER);
		
		// move buffer pointer to end of MultilayerBlock
		for (int cell = 0; cell < GeoStructure.BLOCK_CELLS; cell++)
		{
			// get layer count for this cell
			final byte layers = Config.GEODATA_FORMAT != GeoFormat.L2OFF ? bb.get() : (byte) bb.getShort();
			
			if (layers <= 0 || layers > Byte.MAX_VALUE)
				throw new RuntimeException("Invalid layer count for MultilayerBlock");
			
			// add layers count
			_list.put(layers);
			
			// loop over layers
			for (byte layer = 0; layer < layers; layer++)
			{
				// depending on geodata format
				if (Config.GEODATA_FORMAT != GeoFormat.L2D)
				{
					// get data
					short data = bb.getShort();
					
					// get nswe
					_list.put((byte) (data & 0x000F));
					
					// get height
					data = (short) ((short) (data & 0xFFF0) >> 1);
					_list.put((byte) (data & 0x00FF));
					_list.put((byte) (data >> 8));
				}
				else
				{
					// get nswe
					byte nswe = bb.get();
					_list.put(nswe);
					
					// get height
					short height = bb.getShort();
					_list.put((byte) (height & 0x00FF));
					_list.put((byte) (height >> 8));
				}
			}
		}
		
		// initialize buffer
		final byte[] buffer = Arrays.copyOf(_list.array(), _list.position());
		
		// clear temp buffer
		_list.clear();
		
		return buffer;
	}
	
	/**
	 * Loads null blocks. Used when no region file is detected or an error occurs during loading.
	 * @param regionX : Geodata file region X coordinate.
	 * @param regionY : Geodata file region Y coordinate.
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
		// get block
		final byte[] block = _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY / GeoStructure.BLOCK_CELLS_Y];
		
		// process block
		switch (block[0])
		{
			case GeoStructure.NULL:
				return false;
				
			case GeoStructure.FLAT:
			case GeoStructure.COMPLEX:
			case GeoStructure.MULTILAYER:
				return true;
				
			default:
				throw new IllegalArgumentException("Unknown geodata block type.");
		}
	}
	
	@Override
	public short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		// get block
		final byte[] block = _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY / GeoStructure.BLOCK_CELLS_Y];
		
		int index;
		
		// process block
		switch (block[0])
		{
			case GeoStructure.NULL:
				return (short) worldZ;
				
			case GeoStructure.FLAT:
				index = 1;
				break;
			
			case GeoStructure.COMPLEX:
				index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)) * 3 + 2;
				break;
			
			case GeoStructure.MULTILAYER:
				index = getCellIndexNearest(geoX, geoY, worldZ, block) + 1;
				break;
			
			default:
				throw new IllegalArgumentException("Unknown geodata block type.");
		}
		
		// return Z
		return (short) (block[index] & 0x00FF | block[index + 1] << 8);
	}
	
	@Override
	public short getHeightAbove(int geoX, int geoY, int worldZ)
	{
		// get block
		final byte[] block = _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY / GeoStructure.BLOCK_CELLS_Y];
		
		int index;
		
		// process block
		switch (block[0])
		{
			case GeoStructure.NULL:
				return (short) worldZ;
				
			case GeoStructure.FLAT:
				index = 1;
				break;
			
			case GeoStructure.COMPLEX:
				index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)) * 3 + 2;
				break;
			
			case GeoStructure.MULTILAYER:
				index = getCellIndexAbove(geoX, geoY, worldZ, block) + 1;
				break;
			
			default:
				throw new IllegalArgumentException("Unknown geodata block type.");
		}
		
		// return Z
		return (short) (block[index] & 0x00FF | block[index + 1] << 8);
	}
	
	@Override
	public short getHeightBelow(int geoX, int geoY, int worldZ)
	{
		// get block
		final byte[] block = _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY / GeoStructure.BLOCK_CELLS_Y];
		
		int index;
		
		// process block
		switch (block[0])
		{
			case GeoStructure.NULL:
				return (short) worldZ;
				
			case GeoStructure.FLAT:
				index = 1;
				break;
			
			case GeoStructure.COMPLEX:
				index = ((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)) * 3 + 2;
				break;
			
			case GeoStructure.MULTILAYER:
				index = getCellIndexBelow(geoX, geoY, worldZ, block) + 1;
				break;
			
			default:
				throw new IllegalArgumentException("Unknown geodata block type.");
		}
		
		// return Z
		return (short) (block[index] & 0x00FF | block[index + 1] << 8);
	}
	
	@Override
	public byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		// get block
		final byte[] block = _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY / GeoStructure.BLOCK_CELLS_Y];
		
		// process block
		switch (block[0])
		{
			case GeoStructure.NULL:
			case GeoStructure.FLAT:
				return _nswe;
				
			case GeoStructure.COMPLEX:
				return block[((geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y)) * 3 + 1];
				
			case GeoStructure.MULTILAYER:
				return block[getCellIndexNearest(geoX, geoY, worldZ, block)];
				
			default:
				throw new IllegalArgumentException("Unknown geodata block type.");
		}
	}
	
	/**
	 * Returns cell data of the cell in closes layer to given coordinates.
	 * @param geoX : Geo X.
	 * @param geoY : Geo Y.
	 * @param worldZ : World Z.
	 * @param buffer : Multilayer block data buffer.
	 * @return int : Cell index.
	 */
	private static final int getCellIndexNearest(int geoX, int geoY, int worldZ, byte[] buffer)
	{
		// move buffer index to cell
		int index = 1;
		for (int i = 0; i < (geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y); i++)
		{
			// move index by amount of layers for this cell
			index += buffer[index] * 3 + 1;
		}
		
		// get layers count
		byte layers = buffer[index++];
		
		// loop though all cell layers, find closest layer and return cell index
		int limit = Integer.MAX_VALUE;
		while (layers-- > 0)
		{
			// get layer height
			final int height = buffer[index + 1] & 0x00FF | buffer[index + 2] << 8;
			
			// get Z distance and compare with limit
			final int distance = Math.abs(height - worldZ);
			if (distance >= limit)
				break;
			
			// update distance
			limit = distance;
			
			// move index to next layer
			index += 3;
		}
		
		// last layer
		return index - 3;
	}
	
	/**
	 * Returns cell data of the cell in closes layer to given coordinates.
	 * @param geoX : Geo X.
	 * @param geoY : Geo Y.
	 * @param worldZ : World Z.
	 * @param buffer : Multilayer block data buffer.
	 * @return int : Cell index.
	 * @throws IndexOutOfBoundsException : When cell is above given Z coordinate.
	 */
	private static final int getCellIndexAbove(int geoX, int geoY, int worldZ, byte[] buffer)
	{
		// move buffer index to cell
		int index = 1;
		for (int i = 0; i < (geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y); i++)
		{
			// move index by amount of layers for this cell
			index += buffer[index] * 3 + 1;
		}
		
		// get layers count and shift to first layer data
		short layers = buffer[index++];
		
		// loop though all cell layers, find closest layer above worldZ
		while (layers-- > 0)
		{
			// get layer height and compare height with worldZ
			final short height = (short) (buffer[index + 1] & 0x00FF | buffer[index + 2] << 8);
			if (height > worldZ)
				break;
			
			// set index and move to next layer
			index += 3;
		}
		
		// none layer found
		if (layers < 0)
			throw new IndexOutOfBoundsException();
		
		// return index (it is first, which is above given worldZ)
		return index;
	}
	
	/**
	 * Returns cell data of the cell in closes layer to given coordinates.
	 * @param geoX : Geo X.
	 * @param geoY : Geo Y.
	 * @param worldZ : World Z.
	 * @param buffer : Multilayer block data buffer.
	 * @return int : Cell index.
	 * @throws IndexOutOfBoundsException : When cell is above given Z coordinate.
	 */
	private static final int getCellIndexBelow(int geoX, int geoY, int worldZ, byte[] buffer)
	{
		// move buffer index to cell
		int index = 1;
		for (int i = 0; i < (geoX % GeoStructure.BLOCK_CELLS_X) * GeoStructure.BLOCK_CELLS_Y + (geoY % GeoStructure.BLOCK_CELLS_Y); i++)
		{
			// move index by amount of layers for this cell
			index += buffer[index] * 3 + 1;
		}
		
		// get layers count and shift to first layer data
		short layers = buffer[index++];
		
		// loop though all cell layers, find closest layer above worldZ
		int result = -1;
		while (layers-- > 0)
		{
			// get layer height and compare height with worldZ
			final short height = (short) (buffer[index + 1] & 0x00FF | buffer[index + 2] << 8);
			if (height >= worldZ)
				break;
			
			// set index and move to next layer
			result = index;
			index += 3;
		}
		
		// none layer found
		if (result < 0)
			throw new IndexOutOfBoundsException();
		
		// return index (it is first, which is below given worldZ)
		return result;
	}
}