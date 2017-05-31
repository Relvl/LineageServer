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
package net.sf.l2j.gameserver.model.zone.type;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.ZoneId;

/**
 * @author DaRkRaGe
 */
public class L2BossZone extends L2ZoneType
{
	// Track the times that players got disconnected. Players are allowed to log back into the zone as long as their log-out was within _timeInvade time...
	private final Map<Integer, Long> _playerAllowEntry = new ConcurrentHashMap<>();
	
	// Track players admitted to the zone who should be allowed back in after reboot/server downtime, within 30min of server restart
	private final List<Integer> _playerAllowed = new CopyOnWriteArrayList<>();
	
	private int _timeInvade;
	private boolean _enabled = true;
	private final int[] _oustLoc = new int[3];
	
	public L2BossZone(int id)
	{
		super(id);
		
		GrandBossManager.getInstance().addZone(this);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("InvadeTime"))
			_timeInvade = Integer.parseInt(value);
		else if (name.equals("EnabledByDefault"))
			_enabled = Boolean.parseBoolean(value);
		else if (name.equals("oustX"))
			_oustLoc[0] = Integer.parseInt(value);
		else if (name.equals("oustY"))
			_oustLoc[1] = Integer.parseInt(value);
		else if (name.equals("oustZ"))
			_oustLoc[2] = Integer.parseInt(value);
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (_enabled)
		{
			if (character instanceof L2PcInstance)
			{
				// Get player and set zone info.
				final L2PcInstance player = (L2PcInstance) character;
				player.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
				
				// Skip other checks for GM.
				if (player.isGM())
					return;
				
				// Get player object id.
				final int id = player.getObjectId();
				
				if (_playerAllowed.contains(id))
				{
					// Get and remove the entry expiration time (once entered, can not enter enymore, unless specified).
					final long entryTime = _playerAllowEntry.remove(id);
					if (entryTime > System.currentTimeMillis())
						return;
					
					// Player trying to join after expiration, remove from allowed list.
					_playerAllowed.remove(Integer.valueOf(id));
				}
				
				// Teleport out player, who attempt "illegal" (re-)entry.
				if (_oustLoc[0] != 0 && _oustLoc[1] != 0 && _oustLoc[2] != 0)
					player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], 0);
				else
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			else if (character instanceof L2Summon)
			{
				final L2PcInstance player = ((L2Summon) character).getOwner();
				if (player != null)
				{
					if (_playerAllowed.contains(player.getObjectId()) || player.isGM())
						return;
					
					// Teleport out owner who attempt "illegal" (re-)entry.
					if (_oustLoc[0] != 0 && _oustLoc[1] != 0 && _oustLoc[2] != 0)
						player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], 0);
					else
						player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				
				// Remove summon.
				((L2Summon) character).unSummon(player);
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (character instanceof L2Playable && _enabled)
		{
			if (character instanceof L2PcInstance)
			{
				// Get player and set zone info.
				final L2PcInstance player = (L2PcInstance) character;
				player.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
				
				// Skip other checks for GM.
				if (player.isGM())
					return;
				
				// Get player object id.
				final int id = player.getObjectId();
				
				if (_playerAllowed.contains(id))
				{
					if (!player.isOnline())
					{
						// Player disconnected.
						_playerAllowEntry.put(id, System.currentTimeMillis() + _timeInvade);
					}
					else
					{
						// Player has allowed entry, do not delete from allowed list.
						if (_playerAllowEntry.containsKey(id))
							return;
						
						// Remove player allowed list.
						_playerAllowed.remove(Integer.valueOf(id));
					}
				}
			}
			
			// If playables aren't found, force all bosses to return to spawnpoint.
			if (!_characterList.isEmpty())
			{
				if (!getKnownTypeInside(L2Playable.class).isEmpty())
					return;
				
				for (L2Attackable raid : getKnownTypeInside(L2Attackable.class))
				{
					if (raid.isRaid())
					{
						if (raid.getSpawn() == null || raid.isDead())
							continue;
						
						if (!raid.isInsideRadius(raid.getSpawn().getLocx(), raid.getSpawn().getLocy(), 150, false))
							raid.returnHome();
					}
				}
			}
		}
		else if (character instanceof L2Attackable && character.isRaid() && !character.isDead())
			((L2Attackable) character).returnHome();
	}
	
	/**
	 * Enables the entry of a player to the boss zone for next "duration" seconds. If the player tries to enter the boss zone after this period, he will be teleported out.
	 * @param player : Player to allow entry.
	 * @param duration : Entry permission is valid for this period (in seconds).
	 */
	public void allowPlayerEntry(L2PcInstance player, int duration)
	{
		// Get player object id.
		final int playerId = player.getObjectId();
		
		// Allow player entry.
		if (!_playerAllowed.contains(playerId))
			_playerAllowed.add(playerId);
		
		// For the given duration.
		_playerAllowEntry.put(playerId, System.currentTimeMillis() + duration * 1000);
	}
	
	/**
	 * Enables the entry of a player to the boss zone after server shutdown/restart. The time limit is specified by each zone via "InvadeTime" parameter. If the player tries to enter the boss zone after this period, he will be teleported out.
	 * @param playerId : The ID of player to allow entry.
	 */
	public void allowPlayerEntry(int playerId)
	{
		// Allow player entry.
		if (!_playerAllowed.contains(playerId))
			_playerAllowed.add(playerId);
		
		// For the given duration.
		_playerAllowEntry.put(playerId, System.currentTimeMillis() + _timeInvade);
	}
	
	/**
	 * Removes the player from allowed list and cancel the entry permition.
	 * @param player : Player to remove from the zone.
	 */
	public void removePlayer(L2PcInstance player)
	{
		// Get player object id.
		final int id = player.getObjectId();
		
		// Remove player from allowed list.
		_playerAllowed.remove(Integer.valueOf(id));
		
		// Remove player permission.
		_playerAllowEntry.remove(id);
	}
	
	/**
	 * @return the list of all allowed players object ids.
	 */
	public List<Integer> getAllowedPlayers()
	{
		return _playerAllowed;
	}
	
	/**
	 * Some GrandBosses send all players in zone to a specific part of the zone, rather than just removing them all. If this is the case, this command should be used. If this is no the case, then use oustAllPlayers().
	 * @param x
	 * @param y
	 * @param z
	 */
	public void movePlayersTo(int x, int y, int z)
	{
		if (_characterList.isEmpty())
			return;
		
		for (L2PcInstance player : getKnownTypeInside(L2PcInstance.class))
		{
			if (player.isOnline())
				player.teleToLocation(x, y, z, 0);
		}
	}
	
	/**
	 * Occasionally, all players need to be sent out of the zone (for example, if the players are just running around without fighting for too long, or if all players die, etc). This call sends all online players to town and marks offline players to be teleported (by clearing their relog expiration
	 * times) when they log back in (no real need for off-line teleport).
	 */
	public void oustAllPlayers()
	{
		if (_characterList.isEmpty())
			return;
		
		for (L2PcInstance player : getKnownTypeInside(L2PcInstance.class))
		{
			if (player.isOnline())
			{
				if (_oustLoc[0] != 0 && _oustLoc[1] != 0 && _oustLoc[2] != 0)
					player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2], 0);
				else
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
		}
		_playerAllowEntry.clear();
		_playerAllowed.clear();
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
}