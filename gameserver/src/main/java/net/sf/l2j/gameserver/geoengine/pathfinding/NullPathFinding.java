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

import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.model.Location;

/**
 * @author Hasha
 */
public class NullPathFinding extends PathFinding
{
	public NullPathFinding()
	{
		_log.log(Level.INFO, "NullPathFinding: Ready.");
	}
	
	@Override
	public List<Location> findPath(int ox, int oy, int oz, int tx, int ty, int tz, boolean playable)
	{
		return null;
	}
	
	@Override
	public boolean canSeeTarget(int ox, int oy, int oz, int oheight, int tx, int ty, int tz, int theight)
	{
		return !DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz);
	}
	
	@Override
	public boolean canMoveToTarget(int ox, int oy, int oz, int tx, int ty, int tz)
	{
		return !DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz);
	}
	
	@Override
	public Location canMoveToTargetLoc(int ox, int oy, int oz, int tx, int ty, int tz)
	{
		if (DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz))
			return new Location(ox, oy, oz);
		
		return new Location(tx, ty, tz);
	}
	
	@Override
	public List<String> getStat()
	{
		return null;
	}
}
