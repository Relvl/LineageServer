package net.sf.l2j.gameserver.geoengine.converter;

import net.sf.l2j.Config;
import net.sf.l2j.Server;
import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.gameserver.geoengine.converter.blocks.*;
import net.sf.l2j.gameserver.geoengine.geodata.GeoFormat;
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;
import net.sf.l2j.gameserver.model.world.L2World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import java.util.logging.LogManager;

public final class GeoDataConverter extends Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoDataConverter.class);

    private static final int IGNORE_CELLS_VERTICAL = 3;

    private Block[][] _blocks;

    public GeoDataConverter() throws Exception {
        Server.serverMode = Server.MODE_GAMESERVER;

        final String LOG_FOLDER = "./log"; // Name of folder for log file
        final String LOG_NAME = "config/log.cfg"; // Name of log file

        // Create log folder
        File logFolder = new File(LOG_FOLDER);
        logFolder.mkdir();

        // Create input stream for log file -- or store file data into memory
        InputStream is = new FileInputStream(new File(LOG_NAME));
        LogManager.getLogManager().readConfiguration(is);
        is.close();

        // Initialize config
        Config.load();

        // load geodata and pathfinding
        loadConvertAndSave();
    }

    /**
     * Load region geodata file, perform conversion to diagonal geodata type and store as diagonal geodata file.
     */
    public final void loadConvertAndSave() {
        // initialize geodata container
        _blocks = new Block[GeoStructure.REGION_BLOCKS_X][GeoStructure.REGION_BLOCKS_Y];

        // get geodata type
        String type = "";

        try (Scanner scn = new Scanner(System.in)) {
            while (!(type.equalsIgnoreCase("J") || type.equalsIgnoreCase("O"))) {
                System.out.print("Select geodata type to convert [J..L2J (*.l2j), O..L2OFF (*.dat)]: ");
                type = scn.next();
            }
        }

        Config.GEODATA_FORMAT = type.equalsIgnoreCase("J") ? GeoFormat.L2J : GeoFormat.L2OFF;

        LOGGER.info("GeoDataConverter: Converting all {} according to listing in \"geoengine.properties\" config file.", Config.GEODATA_FORMAT.toString());

        // load geo files according to geoengine config setup
        final ExProperties props = Config.load(Config.GEOENGINE_FILE);
        int converted = 0;
        for (int rx = L2World.TILE_X_MIN; rx <= L2World.TILE_X_MAX; rx++) {
            for (int ry = L2World.TILE_Y_MIN; ry <= L2World.TILE_Y_MAX; ry++) {
                if (props.containsKey(String.valueOf(rx) + "_" + String.valueOf(ry))) {
                    // load geodata
                    if (!loadGeoBlocks(rx, ry)) {
                        LOGGER.warn("GeoDataConverter: Unable to load {} region file.", String.format(Config.GEODATA_FORMAT.getFilename(), rx, ry));
                        continue;
                    }

                    // recalculate nswe
                    if (!recalculateNSWE(rx, ry)) {
                        LOGGER.warn("GeoDataConverter: Unable to convert {} region file.", String.format(Config.GEODATA_FORMAT.getFilename(), rx, ry));
                        continue;
                    }

                    // save geodata
                    if (!saveGeoBlocks(rx, ry)) {
                        LOGGER.warn("GeoDataConverter: Unable to save {} region file.", String.format(GeoFormat.L2D.getFilename(), rx, ry));
                        continue;
                    }

                    converted++;
                    LOGGER.info("GeoDataConverter: Created {} region file.", String.format(GeoFormat.L2D.getFilename(), rx, ry));
                }
            }
        }

        LOGGER.info("GeoDataConverter: Converted {} {} to L2D region file(s).", converted, Config.GEODATA_FORMAT.toString());
    }

    /**
     * Loads geo blocks from buffer of the region file.
     *
     * @param rx : First block of the region X coordinate.
     * @param ry : First block of the region Y coordinate.
     * @return boolean : True when successful.
     */
    private final boolean loadGeoBlocks(int rx, int ry) {
        // standard load
        final String filename = String.format(Config.GEODATA_FORMAT.getFilename(), rx, ry);

        // region file is load-able, try to load it
        try (RandomAccessFile raf = new RandomAccessFile(Config.GEODATA_PATH + filename, "r"); FileChannel fc = raf.getChannel()) {
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).load();
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // load 18B header for L2off geodata (1st and 2nd byte...region X and Y)
            if (Config.GEODATA_FORMAT == GeoFormat.L2OFF) {
                for (int i = 0; i < 18; i++) { buffer.get(); }
            }

            // loop over region blocks
            for (int ix = 0; ix < GeoStructure.REGION_BLOCKS_X; ix++) {
                for (int iy = 0; iy < GeoStructure.REGION_BLOCKS_Y; iy++) {
                    if (Config.GEODATA_FORMAT == GeoFormat.L2J) {
                        // get block type
                        final byte type = buffer.get();

                        // load block according to block type
                        switch (type) {
                            case GeoStructure.TYPE_FLAT_L2J_L2OFF:
                                _blocks[ix][iy] = new FlatBlock(buffer);
                                break;

                            case GeoStructure.TYPE_COMPLEX_L2J:
                                _blocks[ix][iy] = new ComplexBlock(buffer);
                                break;

                            case GeoStructure.TYPE_MULTILAYER_L2J:
                                _blocks[ix][iy] = new MultilayerBlock(buffer);
                                break;

                            default:
                                throw new IllegalArgumentException("Unknown block type: " + type);
                        }
                    }
                    else {
                        // get block type
                        final short type = buffer.getShort();

                        // load block according to block type
                        switch (type) {
                            case GeoStructure.TYPE_FLAT_L2J_L2OFF:
                                _blocks[ix][iy] = new FlatBlock(buffer);
                                break;

                            case GeoStructure.TYPE_COMPLEX_L2OFF:
                                _blocks[ix][iy] = new ComplexBlock(buffer);
                                break;

                            default:
                                _blocks[ix][iy] = new MultilayerBlock(buffer);
                                break;
                        }
                    }
                }
            }

            if (buffer.remaining() > 0) {
                LOGGER.warn("GeoDataConverter: Region file {} can be corrupted, remaining {} bytes to read.", filename, buffer.remaining());
                return false;
            }

            return true;
        }
        catch (Exception e) {
            LOGGER.error("GeoDataConverter: Error while loading {} region file.", filename, e);

            return false;
        }
    }

    /**
     * Recalculate diagonal flags for the region file.
     *
     * @param rx : Region X coordinate.
     * @param ry : Region Y coordinate.
     * @return boolean : True when successful.
     */
    private final boolean recalculateNSWE(int rx, int ry) {
        try {
            for (int x = 0; x < GeoStructure.REGION_CELLS_X; x++) {
                for (int y = 0; y < GeoStructure.REGION_CELLS_Y; y++) {
                    Block block = _blocks[x / GeoStructure.BLOCK_CELLS_X][y / GeoStructure.BLOCK_CELLS_Y];

                    // update cell of complex block
                    if (block instanceof ComplexBlock) {
                        short height = getHeightNearest(x, y, (short) 0);
                        byte nswe = getNsweBelow(x, y, (short) 0);

                        nswe = updateNsweBelow(x, y, height, nswe);

						/*
                         * // test byte test = updateNsweNearest(x, y, height, nswe); if (nswe != test) { int geoX = (rx - 16) * GeoStructure.REGION_CELLS_X + x; int geoY = (ry - 10) * GeoStructure.REGION_CELLS_Y + y; System.out.println("C: X=" + geoX + " Y=" + geoY + " Z=" + height + " below=" +
						 * Integer.toBinaryString((0x100 | nswe) & 0x1FF).substring(1) + " near=" + Integer.toBinaryString((0x100 | test) & 0x1FF).substring(1)); }
						 */

                        ((ComplexBlock) block).updateNSWE(x, y, nswe);
                    }
                    // update cell of multilayer block
                    else if (block instanceof MultilayerBlock) {
                        MultilayerCell cell = ((MultilayerBlock) block).getGeoCells(x, y);
                        for (int i = 0; i < cell.getLayers(); i++) {
                            short height = cell.getHeight(i);
                            byte nswe = cell.getNSWE(i);

                            nswe = updateNsweBelow(x, y, height, nswe);

							/*
							 * // test byte test = updateNsweNearest(x, y, height, nswe); if (nswe != test) { int geoX = (rx - 16) * GeoStructure.REGION_CELLS_X + x; int geoY = (ry - 10) * GeoStructure.REGION_CELLS_Y + y; System.out.println("M: X=" + geoX + " Y=" + geoY + " Z=" + height + " below="
							 * + Integer.toBinaryString((0x100 | nswe) & 0x1FF).substring(1) + " near=" + Integer.toBinaryString((0x100 | test) & 0x1FF).substring(1)); }
							 */

                            cell.updateNswe(i, nswe);
                        }
                    }
                    // no change for cell of flat block (can move anywhere)
                    else {

                    }
                }
            }

            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    private final short getHeightNearest(int geoX, int geoY, short worldZ) {
        return _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY / GeoStructure.BLOCK_CELLS_Y].getHeightNearest(geoX, geoY, worldZ);
    }
	
	/*
	 * private final byte getNsweNearest(int geoX, int geoY, short worldZ) { if (geoX < 0 || geoX >= GeoStructure.REGION_CELLS_X) return 0; if (geoY < 0 || geoY >= GeoStructure.REGION_CELLS_Y) return 0; return _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY /
	 * GeoStructure.BLOCK_CELLS_Y].getNsweNearest(geoX, geoY, worldZ); }
	 */

    private final byte getNsweBelow(int geoX, int geoY, short worldZ) {
        if (geoX < 0 || geoX >= GeoStructure.REGION_CELLS_X) { return 0; }

        if (geoY < 0 || geoY >= GeoStructure.REGION_CELLS_Y) { return 0; }

        return _blocks[geoX / GeoStructure.BLOCK_CELLS_X][geoY / GeoStructure.BLOCK_CELLS_Y].getNsweBelow(geoX, geoY, worldZ);
    }
	
	/*
	 * private final byte updateNsweNearest(int x, int y, short z, byte nswe) { byte nsweN = getNsweNearest(x, y - 1, z); byte nsweS = getNsweNearest(x, y + 1, z); byte nsweW = getNsweNearest(x - 1, y, z); byte nsweE = getNsweNearest(x + 1, y, z); // North-West if (((nswe & GeoStructure.CELL_FLAG_N)
	 * != 0 && (nsweN & GeoStructure.CELL_FLAG_W) != 0) || ((nswe & GeoStructure.CELL_FLAG_W) != 0 && (nsweW & GeoStructure.CELL_FLAG_N) != 0)) nswe |= GeoStructure.CELL_FLAG_NW; // North-East if (((nswe & GeoStructure.CELL_FLAG_N) != 0 && (nsweN & GeoStructure.CELL_FLAG_E) != 0) || ((nswe &
	 * GeoStructure.CELL_FLAG_E) != 0 && (nsweE & GeoStructure.CELL_FLAG_N) != 0)) nswe |= GeoStructure.CELL_FLAG_NE; // South-West if (((nswe & GeoStructure.CELL_FLAG_S) != 0 && (nsweS & GeoStructure.CELL_FLAG_W) != 0) || ((nswe & GeoStructure.CELL_FLAG_W) != 0 && (nsweW & GeoStructure.CELL_FLAG_S)
	 * != 0)) nswe |= GeoStructure.CELL_FLAG_SW; // South-East if (((nswe & GeoStructure.CELL_FLAG_S) != 0 && (nsweS & GeoStructure.CELL_FLAG_E) != 0) || ((nswe & GeoStructure.CELL_FLAG_E) != 0 && (nsweE & GeoStructure.CELL_FLAG_S) != 0)) nswe |= GeoStructure.CELL_FLAG_SE; return nswe; }
	 */

    /**
     * Updates the NSWE flag with diagonal flags.
     *
     * @param x    : Geodata X coordinate.
     * @param y    : Geodata Y coordinate.
     * @param z    : Geodata Z coordinate.
     * @param nswe : NSWE flag to be updated.
     * @return byte : Updated NSWE flag.
     */
    private final byte updateNsweBelow(int x, int y, short z, byte nswe) {
        // calculate virtual layer height (4 cells above cell height)
        short height = (short) (z + IGNORE_CELLS_VERTICAL * GeoStructure.CELL_SIZE);

        // get NSWE of neighbor cells below virtual layer (NPC/PC can fall down of clif, but can not climb it -> NSWE of cell below)
        byte nsweN = getNsweBelow(x, y - 1, height);
        byte nsweS = getNsweBelow(x, y + 1, height);
        byte nsweW = getNsweBelow(x - 1, y, height);
        byte nsweE = getNsweBelow(x + 1, y, height);

        // North-West
        if (((nswe & GeoStructure.CELL_FLAG_N) != 0 && (nsweN & GeoStructure.CELL_FLAG_W) != 0) || ((nswe & GeoStructure.CELL_FLAG_W) != 0 && (nsweW & GeoStructure.CELL_FLAG_N) != 0)) { nswe |= GeoStructure.CELL_FLAG_NW; }

        // North-East
        if (((nswe & GeoStructure.CELL_FLAG_N) != 0 && (nsweN & GeoStructure.CELL_FLAG_E) != 0) || ((nswe & GeoStructure.CELL_FLAG_E) != 0 && (nsweE & GeoStructure.CELL_FLAG_N) != 0)) { nswe |= GeoStructure.CELL_FLAG_NE; }

        // South-West
        if (((nswe & GeoStructure.CELL_FLAG_S) != 0 && (nsweS & GeoStructure.CELL_FLAG_W) != 0) || ((nswe & GeoStructure.CELL_FLAG_W) != 0 && (nsweW & GeoStructure.CELL_FLAG_S) != 0)) { nswe |= GeoStructure.CELL_FLAG_SW; }

        // South-East
        if (((nswe & GeoStructure.CELL_FLAG_S) != 0 && (nsweS & GeoStructure.CELL_FLAG_E) != 0) || ((nswe & GeoStructure.CELL_FLAG_E) != 0 && (nsweE & GeoStructure.CELL_FLAG_S) != 0)) { nswe |= GeoStructure.CELL_FLAG_SE; }

        return nswe;
    }

    /**
     * Save region file to file.
     *
     * @param rx : First block of the region X coordinate.
     * @param ry : First block of the region Y coordinate.
     * @return boolean : True when successful.
     */
    private final boolean saveGeoBlocks(int rx, int ry) {
        final String filename = String.format(GeoFormat.L2D.getFilename(), rx, ry);

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(Config.GEODATA_PATH + filename))) {
            // loop over region blocks
            for (int ix = 0; ix < GeoStructure.REGION_BLOCKS_X; ix++) {
                for (int iy = 0; iy < GeoStructure.REGION_BLOCKS_Y; iy++) {
                    _blocks[ix][iy].saveBlock(bos);
                }
            }

            bos.flush();

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        new GeoDataConverter();
    }
}