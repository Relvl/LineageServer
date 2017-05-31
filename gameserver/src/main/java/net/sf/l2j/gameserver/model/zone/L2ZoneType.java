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
package net.sf.l2j.gameserver.model.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * Abstract base class for any zone type.
 * @author durgus
 */
public abstract class L2ZoneType
{
	protected static final Logger _log = Logger.getLogger(L2ZoneType.class.getName());
	
	private final int _id;
	protected L2ZoneForm _zone;
	protected List<L2Character> _characterList;
	
	private Map<EventType, List<Quest>> _questEvents;
	
	protected L2ZoneType(int id)
	{
		_id = id;
		_characterList = new CopyOnWriteArrayList<>();
	}
	
	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Setup new parameters for this zone
	 * @param name parameter name.
	 * @param value new parameter value.
	 */
	public void setParameter(String name, String value)
	{
		_log.info(getClass().getSimpleName() + ": Unknown parameter - " + name + " in zone: " + getId());
	}
	
	/**
	 * @param character The character to test.
	 * @return True if the given character is affected by this zone.
	 */
	protected boolean isAffected(L2Character character)
	{
		// Overriden in children classes.
		return true;
	}
	
	/**
	 * Set the zone for this L2ZoneType Instance
	 * @param zone
	 */
	public void setZone(L2ZoneForm zone)
	{
		if (_zone != null)
			throw new IllegalStateException("Zone already set");
		_zone = zone;
	}
	
	/**
	 * @return this zone form.
	 */
	public L2ZoneForm getZone()
	{
		return _zone;
	}
	
	/**
	 * @param x
	 * @param y
	 * @return true if the given coordinates are within zone's plane
	 */
	public boolean isInsideZone(int x, int y)
	{
		return _zone.isInsideZone(x, y, _zone.getHighZ());
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the given coordinates are within the zone
	 */
	public boolean isInsideZone(int x, int y, int z)
	{
		return _zone.isInsideZone(x, y, z);
	}
	
	/**
	 * @param object check object's X/Y positions.
	 * @return true if the given object is inside the zone.
	 */
	public boolean isInsideZone(L2Object object)
	{
		return isInsideZone(object.getX(), object.getY(), object.getZ());
	}
	
	public double getDistanceToZone(int x, int y)
	{
		return getZone().getDistanceToZone(x, y);
	}
	
	public double getDistanceToZone(L2Object object)
	{
		return getZone().getDistanceToZone(object.getX(), object.getY());
	}
	
	public void revalidateInZone(L2Character character)
	{
		// If the character can't be affected by this zone return
		if (!isAffected(character))
			return;
		
		// If the object is inside the zone...
		if (isInsideZone(character.getX(), character.getY(), character.getZ()))
		{
			// Was the character not yet inside this zone?
			if (!_characterList.contains(character))
			{
				List<Quest> quests = getQuestByEvent(EventType.ON_ENTER_ZONE);
				if (quests != null)
				{
					for (Quest quest : quests)
						quest.notifyEnterZone(character, this);
				}
				_characterList.add(character);
				onEnter(character);
			}
		}
		else
		{
			// Was the character inside this zone?
			if (_characterList.contains(character))
			{
				List<Quest> quests = getQuestByEvent(EventType.ON_EXIT_ZONE);
				if (quests != null)
				{
					for (Quest quest : quests)
						quest.notifyExitZone(character, this);
				}
				_characterList.remove(character);
				onExit(character);
			}
		}
	}
	
	/**
	 * Force fully removes a character from the zone Should use during teleport / logoff
	 * @param character
	 */
	public void removeCharacter(L2Character character)
	{
		if (_characterList.contains(character))
		{
			List<Quest> quests = getQuestByEvent(EventType.ON_EXIT_ZONE);
			if (quests != null)
			{
				for (Quest quest : quests)
					quest.notifyExitZone(character, this);
			}
			_characterList.remove(character);
			onExit(character);
		}
	}
	
	/**
	 * @param character The character to test.
	 * @return True if the character is in the zone.
	 */
	public boolean isCharacterInZone(L2Character character)
	{
		return _characterList.contains(character);
	}
	
	protected abstract void onEnter(L2Character character);
	
	protected abstract void onExit(L2Character character);
	
	public abstract void onDieInside(L2Character character);
	
	public abstract void onReviveInside(L2Character character);
	
	public List<L2Character> getCharactersInside()
	{
		return _characterList;
	}
	
	/**
	 * @param <A>
	 * @param type
	 * @return a list of given instances within this zone.
	 */
	@SuppressWarnings("unchecked")
	public final <A> List<A> getKnownTypeInside(Class<A> type)
	{
		List<A> result = new ArrayList<>();
		
		for (L2Object obj : _characterList)
		{
			if (type.isAssignableFrom(obj.getClass()))
				result.add((A) obj);
		}
		return result;
	}
	
	public void addQuestEvent(EventType eventType, Quest quest)
	{
		if (_questEvents == null)
			_questEvents = new HashMap<>();
		
		List<Quest> eventList = _questEvents.get(eventType);
		if (eventList == null)
		{
			eventList = new ArrayList<>();
			eventList.add(quest);
			_questEvents.put(eventType, eventList);
		}
		else
		{
			eventList.remove(quest);
			eventList.add(quest);
		}
	}
	
	public List<Quest> getQuestByEvent(EventType EventType)
	{
		return (_questEvents == null) ? null : _questEvents.get(EventType);
	}
	
	/**
	 * Broadcasts packet to all players inside the zone
	 * @param packet The packet to use.
	 */
	public void broadcastPacket(L2GameServerPacket packet)
	{
		if (_characterList.isEmpty())
			return;
		
		for (L2Character character : _characterList)
		{
			if (character != null && character instanceof L2PcInstance)
				character.sendPacket(packet);
		}
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _id + "]";
	}
	
	public void visualizeZone(int z)
	{
		getZone().visualizeZone(z);
	}
}