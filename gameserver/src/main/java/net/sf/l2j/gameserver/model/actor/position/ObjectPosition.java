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
package net.sf.l2j.gameserver.model.actor.position;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;

public class ObjectPosition
{
	private static final Logger _log = Logger.getLogger(ObjectPosition.class.getName());
	
	private final L2Object _activeObject;
	private Location _worldPosition;
	private L2WorldRegion _worldRegion; // Object localization : Used for items/chars that are seen in the world
	
	public ObjectPosition(L2Object activeObject)
	{
		_activeObject = activeObject;
		setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
	}
	
	/**
	 * Set the x,y,z position of the L2Object and if necessary modify its _worldRegion.
	 * @param x
	 * @param y
	 * @param z
	 */
	public final void setXYZ(int x, int y, int z)
	{
		assert getWorldRegion() != null;
		setWorldPosition(x, y, z);
		
		try
		{
			if (L2World.getInstance().getRegion(getWorldPosition()) != getWorldRegion())
				updateWorldRegion();
		}
		catch (Exception e)
		{
			_log.warning("Object Id at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
			badCoords();
		}
	}
	
	/**
	 * Called on setXYZ exception.<BR>
	 * <BR>
	 * <B><U> Overwritten in </U> :</B><BR>
	 * <BR>
	 * <li>CharPosition</li> <li>PcPosition</li><BR>
	 */
	protected void badCoords()
	{
	}
	
	/**
	 * Set the x,y,z position of the L2Object and make it invisible. A L2Object is invisble if <B>_hidden</B>=true or <B>_worldregion</B>==null
	 * @param x
	 * @param y
	 * @param z
	 */
	public final void setXYZInvisible(int x, int y, int z)
	{
		assert getWorldRegion() == null;
		
		if (x > L2World.WORLD_X_MAX)
			x = L2World.WORLD_X_MAX - 5000;
		if (x < L2World.WORLD_X_MIN)
			x = L2World.WORLD_X_MIN + 5000;
		if (y > L2World.WORLD_Y_MAX)
			y = L2World.WORLD_Y_MAX - 5000;
		if (y < L2World.WORLD_Y_MIN)
			y = L2World.WORLD_Y_MIN + 5000;
		
		setWorldPosition(x, y, z);
		getActiveObject().setIsVisible(false);
	}
	
	/**
	 * checks if current object changed its region, if so, update referencies
	 */
	public void updateWorldRegion()
	{
		if (!getActiveObject().isVisible())
			return;
		
		L2WorldRegion newRegion = L2World.getInstance().getRegion(getWorldPosition());
		if (newRegion != getWorldRegion())
		{
			getWorldRegion().removeVisibleObject(getActiveObject());
			
			setWorldRegion(newRegion);
			
			// Add the L2Oject spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
			getWorldRegion().addVisibleObject(getActiveObject());
		}
	}
	
	public L2Object getActiveObject()
	{
		return _activeObject;
	}
	
	/**
	 * @return the x position of the L2Object.
	 */
	public final int getX()
	{
		return getWorldPosition().getX();
	}
	
	/**
	 * @return the y position of the L2Object.
	 */
	public final int getY()
	{
		return getWorldPosition().getY();
	}
	
	/**
	 * @return the z position of the L2Object.
	 */
	public final int getZ()
	{
		return getWorldPosition().getZ();
	}
	
	public final Location getWorldPosition()
	{
		if (_worldPosition == null)
			_worldPosition = new Location(0, 0, 0);
		
		return _worldPosition;
	}
	
	public final void setWorldPosition(int x, int y, int z)
	{
		getWorldPosition().setXYZ(x, y, z);
	}
	
	public final void setWorldPosition(Location newPosition)
	{
		setWorldPosition(newPosition.getX(), newPosition.getY(), newPosition.getZ());
	}
	
	public final L2WorldRegion getWorldRegion()
	{
		return _worldRegion;
	}
	
	public void setWorldRegion(L2WorldRegion value)
	{
		// confirm revalidation of old region's zones
		if (_worldRegion != null && getActiveObject() instanceof L2Character)
		{
			if (value != null)
				_worldRegion.revalidateZones((L2Character) getActiveObject());
			else
				_worldRegion.removeFromZones((L2Character) getActiveObject());
		}
		
		_worldRegion = value;
	}
}