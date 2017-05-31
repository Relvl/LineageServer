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
package net.sf.l2j.gameserver.geoengine.pathfinding;

import java.util.List;
import java.util.logging.Level;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.geoengine.geodata.GeoStructure;
import net.sf.l2j.gameserver.geoengine.pathfinding.nodes.GeoLocation;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;

/**
 * @author Hasha
 */
public class PathCheckerDiag extends PathFinding
{
	public PathCheckerDiag()
	{
		_log.log(Level.INFO, "PathCheckerDiag: Prepared.");
	}
	
	@Override
	public List<Location> findPath(int ox, int oy, int oz, int tx, int ty, int tz, boolean playable)
	{
		return null;
	}
	
	@Override
	public boolean canSeeTarget(int ox, int oy, int oz, int oheight, int tx, int ty, int tz, int theight)
	{
		// perform door check
		if (DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz))
			return false;
		
		// get origin and check existing geo coords
		int gox = GeoData.getInstance().getGeoX(ox);
		int goy = GeoData.getInstance().getGeoY(oy);
		if (!GeoData.getInstance().hasGeoPos(gox, goy))
			return true;
		
		short goz = GeoData.getInstance().getHeightNearest(gox, goy, oz);
		
		// get target and check existing geo coords
		int gtx = GeoData.getInstance().getGeoX(tx);
		int gty = GeoData.getInstance().getGeoY(ty);
		if (!GeoData.getInstance().hasGeoPos(gtx, gty))
			return true;
		
		short gtz = GeoData.getInstance().getHeightNearest(gtx, gty, tz);
		
		// origin and target coords are same
		if (gox == gtx && goy == gty)
			return goz == gtz;
		
		// perform geodata check
		return checkSee(gox, goy, goz, oheight, gtx, gty, gtz, theight);
	}
	
	@Override
	public boolean canMoveToTarget(int ox, int oy, int oz, int tx, int ty, int tz)
	{
		// perform door check
		if (DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz))
			return false;
		
		// get origin and check existing geo coords
		final int gox = GeoData.getInstance().getGeoX(ox);
		final int goy = GeoData.getInstance().getGeoY(oy);
		if (!GeoData.getInstance().hasGeoPos(gox, goy))
			return true;
		
		final short goz = GeoData.getInstance().getHeightNearest(gox, goy, oz);
		
		// get target and check existing geo coords
		final int gtx = GeoData.getInstance().getGeoX(tx);
		final int gty = GeoData.getInstance().getGeoY(ty);
		if (!GeoData.getInstance().hasGeoPos(gtx, gty))
			return true;
		
		final short gtz = GeoData.getInstance().getHeightNearest(gtx, gty, tz);
		
		// target coords reached
		if (gox == gtx && goy == gty && goz == gtz)
			return true;
		
		// perform geodata check
		GeoLocation loc = checkMove(gox, goy, goz, gtx, gty, gtz);
		return loc.getGeoX() == gtx && loc.getGeoY() == gty;
	}
	
	@Override
	public Location canMoveToTargetLoc(int ox, int oy, int oz, int tx, int ty, int tz)
	{
		if (DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz))
			return new Location(ox, oy, oz);
		
		// get origin and check existing geo coords
		final int gox = GeoData.getInstance().getGeoX(ox);
		final int goy = GeoData.getInstance().getGeoY(oy);
		if (!GeoData.getInstance().hasGeoPos(gox, goy))
			return new Location(tx, ty, tz);
		
		final short goz = GeoData.getInstance().getHeightNearest(gox, goy, oz);
		
		// get target and check existing geo coords
		final int gtx = GeoData.getInstance().getGeoX(tx);
		final int gty = GeoData.getInstance().getGeoY(ty);
		if (!GeoData.getInstance().hasGeoPos(gtx, gty))
			return new Location(tx, ty, tz);
		
		final short gtz = GeoData.getInstance().getHeightNearest(gtx, gty, tz);
		
		// target coords reached
		if (gox == gtx && goy == gty && goz == gtz)
			return new Location(tx, ty, tz);
		
		// perform geodata check
		return checkMove(gox, goy, goz, gtx, gty, gtz);
	}
	
	@Override
	public List<String> getStat()
	{
		return null;
	}
	
	/**
	 * Simple check for origin to target visibility.
	 * @param gox : origin X coord
	 * @param goy : origin Y coord
	 * @param goz : origin Z coord
	 * @param oheight : origin height (if instance of {@link L2Character})
	 * @param gtx : target X coord
	 * @param gty : target Y coord
	 * @param gtz : target Z coord
	 * @param theight : target height (if instance of {@link L2Character})
	 * @return boolean : can see the target
	 */
	private static final boolean checkSee(int gox, int goy, int goz, int oheight, int gtx, int gty, int gtz, int theight)
	{
		// PathFinding.clearDebugItems();
		
		// get line of sight Z coords
		double noz = goz + (double) oheight * Config.PART_OF_CHARACTER_HEIGHT / 100;
		double ntz = gtz + (double) theight * Config.PART_OF_CHARACTER_HEIGHT / 100;
		
		// get X delta and signum
		final int dx = Math.abs(gtx - gox);
		final int sx = gox < gtx ? 1 : -1;
		
		// get Y delta and signum
		final int dy = Math.abs(gty - goy);
		final int sy = goy < gty ? 1 : -1;
		
		// get Z delta
		final int dm = Math.max(dx, dy);
		final double dz = (ntz - noz) / dm;
		
		// delta, determines axis to move on (+..X axis, -..Y axis)
		int d = dx - dy;
		
		// PathFinding.dropDebugItem(57, 0, new GeoLocation(gox, goy, goz));
		// PathFinding.dropDebugItem(1831, 0, new GeoLocation(gtx, gty, gtz));
		
		// loop
		for (int i = 0; i < (dm + 1) / 2; i++)
		{
			int e2 = 2 * d;
			if (e2 > -dy)
			{
				// calculate next point X coord
				d -= dy;
				gox += sx;
				gtx -= sx;
			}
			
			if (e2 < dx)
			{
				// calculate next point Y coord
				d += dx;
				goy += sy;
				gty -= sy;
			}
			
			// calculate next point Z coord
			goz = GeoData.getInstance().getHeightNearest(gox, goy, goz);
			gtz = GeoData.getInstance().getHeightNearest(gtx, gty, gtz);
			
			// PathFinding.dropDebugItem(57, 0, new GeoLocation(gox, goy, goz));
			// PathFinding.dropDebugItem(1831, 0, new GeoLocation(gtx, gty, gtz));
			
			// calculate next line of sight Z coord
			noz += dz;
			ntz -= dz;
			
			// perform line of sight check
			if ((goz - noz) > Config.MAX_OBSTACLE_HEIGHT || (gtz - ntz) > Config.MAX_OBSTACLE_HEIGHT)
			{
				// GmListTable.broadcastMessageToGMs("can not see");
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * With this method you can check if a position is visible or can be reached by beeline movement.<br>
	 * Target X and Y reachable and Z is on same floor:
	 * <ul>
	 * <li>Location of the target with corrected Z value from geodata.</li>
	 * </ul>
	 * Target X and Y reachable but Z is on another floor:
	 * <ul>
	 * <li>Location of the origin with corrected Z value from geodata.</li>
	 * </ul>
	 * Target X and Y not reachable:
	 * <ul>
	 * <li>Last accessible location in destination to target.</li>
	 * </ul>
	 * @param gox : origin X geodata coord
	 * @param goy : origin Y geodata coord
	 * @param goz : origin Z geodata coord
	 * @param gtx : target X geodata coord
	 * @param gty : target Y geodata coord
	 * @param gtz : target Z geodata coord
	 * @return GeoLocation : The last allowed point of movement.
	 */
	protected static final GeoLocation checkMove(int gox, int goy, int goz, int gtx, int gty, int gtz)
	{
		// PathFinding.clearDebugItems();
		
		// get X delta, signum and direction flag
		final int dx = Math.abs(gtx - gox);
		final int sx = gox < gtx ? 1 : -1;
		final byte dirX = sx > 0 ? GeoStructure.CELL_FLAG_E : GeoStructure.CELL_FLAG_W;
		
		// get Y delta, signum and direction flag
		final int dy = Math.abs(gty - goy);
		final int sy = goy < gty ? 1 : -1;
		final byte dirY = sy > 0 ? GeoStructure.CELL_FLAG_S : GeoStructure.CELL_FLAG_N;
		
		// get direction flag for diagonal movement
		final byte dirXY = getDirXY(dirX, dirY);
		
		// delta, determines axis to move on (+..X axis, -..Y axis)
		int d = dx - dy;
		
		// NSWE direction of movement
		byte direction;
		
		// load pointer coords
		int gpx = gox;
		int gpy = goy;
		int gpz = goz;
		
		// load next pointer
		int nx = gpx;
		int ny = gpy;
		
		// PathFinding.dropDebugItem(57, 0, new GeoLocation(gpx, gpy, (short) gpz));
		
		// loop
		do
		{
			direction = 0;
			
			// calculate next point coords
			int e2 = 2 * d;
			if (e2 > -dy && e2 < dx)
			{
				d -= dy;
				d += dx;
				nx += sx;
				ny += sy;
				direction |= dirXY;
			}
			else if (e2 > -dy)
			{
				d -= dy;
				nx += sx;
				direction |= dirX;
			}
			else if (e2 < dx)
			{
				d += dx;
				ny += sy;
				direction |= dirY;
			}
			
			// obstacle found, return
			if ((GeoData.getInstance().getNsweNearest(gpx, gpy, gpz) & direction) == 0)
				return new GeoLocation(gpx, gpy, gpz);
			
			// update pointer coords
			gpx = nx;
			gpy = ny;
			gpz = GeoData.getInstance().getHeightNearest(nx, ny, gpz);
			
			// PathFinding.dropDebugItem(57, 0, new GeoLocation(gpx, gpy, (short) gpz));
			
			// target coords reached
			if (gpx == gtx && gpy == gty)
			{
				if (gpz == gtz)
				{
					// path found, Z coords are okay, return target point
					return new GeoLocation(gtx, gty, gtz);
				}
				
				// path found, Z coords are not okay, return origin point
				return new GeoLocation(gox, goy, goz);
			}
		}
		while (true);
	}
	
	/**
	 * Returns diagonal NSWE flag format of combined two NSWE flags.
	 * @param dirX : X direction NSWE flag
	 * @param dirY : Y direction NSWE flag
	 * @return byte : NSWE flag of combined direction
	 */
	private static final byte getDirXY(byte dirX, byte dirY)
	{
		// check axis directions
		if (dirY == GeoStructure.CELL_FLAG_N)
		{
			if (dirX == GeoStructure.CELL_FLAG_W)
				return GeoStructure.CELL_FLAG_NW;
			
			return GeoStructure.CELL_FLAG_NE;
		}
		
		if (dirX == GeoStructure.CELL_FLAG_W)
			return GeoStructure.CELL_FLAG_SW;
		
		return GeoStructure.CELL_FLAG_SE;
	}
}