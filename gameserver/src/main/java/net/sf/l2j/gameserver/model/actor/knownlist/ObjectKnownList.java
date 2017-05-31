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
package net.sf.l2j.gameserver.model.actor.knownlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.util.Util;

public class ObjectKnownList
{
	protected final L2Object _activeObject;
	protected final Map<Integer, L2Object> _knownObjects;
	
	public ObjectKnownList(L2Object activeObject)
	{
		_activeObject = activeObject;
		_knownObjects = new ConcurrentHashMap<>();
	}
	
	/**
	 * Add object to known list.<br>
	 * <b>Is overridden by children in most cases.</b>
	 * @param object : {@link L2Object} to be added.
	 * @return boolean : True, when object was successfully added.
	 */
	public boolean addKnownObject(L2Object object)
	{
		// object must exist, cannot add self
		if (knowsObject(object))
			return false;
		
		// object must be inside distance to watch
		if (!Util.checkIfInShortRadius(getDistanceToWatchObject(object), _activeObject, object, true))
			return false;
		
		// add object to known list and check if object already existed there
		return _knownObjects.put(object.getObjectId(), object) == null;
	}
	
	/**
	 * Remove object from known list.<br>
	 * <b>Is overridden by children in most cases.</b>
	 * @param object : {@link L2Object} to be removed.
	 * @return boolean : True, when object was successfully removed.
	 */
	public boolean removeKnownObject(L2Object object)
	{
		// object must exist.
		if (object == null)
			return false;
		
		// remove object from known list and check if object existed in there
		return _knownObjects.remove(object.getObjectId()) != null;
	}
	
	/**
	 * Remove object from known list, which are beyond distance to forget.
	 */
	public final void forgetObjects()
	{
		// for all objects in known list
		for (L2Object object : _knownObjects.values())
		{
			// object is not visible or out of distance to forget, remove from known list
			if (!object.isVisible() || !Util.checkIfInShortRadius(getDistanceToForgetObject(object), _activeObject, object, true))
				removeKnownObject(object);
		}
	}
	
	/**
	 * Remove all objects from known list.
	 */
	public void removeAllKnownObjects()
	{
		_knownObjects.clear();
	}
	
	/**
	 * Check if object is in known list.
	 * @param object : {@link L2Object} to be checked.
	 * @return boolean : True, when object is in known list.
	 */
	public final boolean knowsObject(L2Object object)
	{
		// object does not exist, false
		if (object == null)
			return false;
		
		// object is known list owner or is in known list
		return _activeObject == object || _knownObjects.containsKey(object.getObjectId());
	}
	
	/**
	 * Return the known list.
	 * @return Collection<L2Object> : The known list.
	 */
	public final Collection<L2Object> getKnownObjects()
	{
		return _knownObjects.values();
	}
	
	/**
	 * Return the known list of given object type.
	 * @param <A> : Object type must be instance of {@link L2Object}.
	 * @param type : Class specifying object type.
	 * @return List<A> : Known list of given object type.
	 */
	@SuppressWarnings("unchecked")
	public final <A> List<A> getKnownType(Class<A> type)
	{
		// create result list
		List<A> result = new ArrayList<>();
		
		// for all objects in known list
		for (L2Object obj : _knownObjects.values())
		{
			// object type is correct, add to the list
			if (type.isAssignableFrom(obj.getClass()))
				result.add((A) obj);
		}
		
		// return result
		return result;
	}
	
	/**
	 * Return the known list of given object type within specified radius.
	 * @param <A> : Object type must be instance of {@link L2Object}.
	 * @param type : Class specifying object type.
	 * @param radius : Radius to in which object must be located.
	 * @return List<A> : Known list of given object type.
	 */
	@SuppressWarnings("unchecked")
	public final <A> List<A> getKnownTypeInRadius(Class<A> type, int radius)
	{
		// create result list
		List<A> result = new ArrayList<>();
		
		// for all objects in known list
		for (L2Object obj : _knownObjects.values())
		{
			// object type is correct and object in given radius, add to the list
			if (type.isAssignableFrom(obj.getClass()) && Util.checkIfInRange(radius, _activeObject, obj, true))
				result.add((A) obj);
		}
		
		// return result
		return result;
	}
	
	/**
	 * Returns the distance to watch object, aka distance to add object to known list.<br>
	 * <b>Is overridden by children in most cases.</b>
	 * @param object : {@link L2Object} to be checked.
	 * @return int : Distance.
	 */
	public int getDistanceToWatchObject(L2Object object)
	{
		return 0;
	}
	
	/**
	 * Returns the distance to forget object, aka distance to remove object from known list.<br>
	 * <b>Is overridden by children in most cases.</b>
	 * @param object : {@link L2Object} to be checked.
	 * @return int : Distance.
	 */
	public int getDistanceToForgetObject(L2Object object)
	{
		return 0;
	}
}