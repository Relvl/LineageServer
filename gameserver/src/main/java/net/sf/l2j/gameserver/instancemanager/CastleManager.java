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
package net.sf.l2j.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.CastleUpdater;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

public class CastleManager
{
	protected static final Logger _log = Logger.getLogger(CastleManager.class.getName());
	
	public static final CastleManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static final int _castleCirclets[] =
	{
		0,
		6838,
		6835,
		6839,
		6837,
		6840,
		6834,
		6836,
		8182,
		8183
	};
	
	private final List<Castle> _castles = new ArrayList<>();
	
	protected CastleManager()
	{
	}
	
	public final int findNearestCastleIndex(L2Object obj)
	{
		int index = getCastleIndex(obj);
		if (index < 0)
		{
			double closestDistance = 99999999;
			
			for (int i = 0; i < _castles.size(); i++)
			{
				Castle castle = _castles.get(i);
				if (castle == null)
					continue;
				
				double distance = castle.getDistance(obj);
				if (closestDistance > distance)
				{
					closestDistance = distance;
					index = i;
				}
			}
		}
		return index;
	}
	
	public final void load()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle ORDER BY id");
			ResultSet rs = statement.executeQuery();
			
			PreparedStatement statement2 = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ?");
			while (rs.next())
			{
				// Create a new Castle object, and populate it with data.
				final Castle castle = new Castle();
				final int id = rs.getInt("id");
				
				castle.setCastleId(id);
				castle.setName(rs.getString("name"));
				
				castle.setSiegeDate(Calendar.getInstance());
				castle.getSiegeDate().setTimeInMillis(rs.getLong("siegeDate"));
				
				castle.setSiegeRegistrationEndDate(Calendar.getInstance());
				castle.getSiegeRegistrationEndDate().setTimeInMillis(rs.getLong("regTimeEnd"));
				
				castle.setTimeRegistrationOver(rs.getBoolean("regTimeOver"));
				castle.setTaxPercent(rs.getInt("taxPercent"), false);
				castle.setTreasury(rs.getLong("treasury"));
				
				// Retrieve clan owner, if any.
				statement2.setInt(1, id);
				ResultSet rs2 = statement2.executeQuery();
				statement2.clearParameters();
				
				while (rs2.next())
				{
					final int ownerId = rs2.getInt("clan_id");
					if (ownerId > 0)
					{
						// Try to find clan instance
						final L2Clan clan = ClanTable.getInstance().getClan(ownerId);
						if (clan != null)
						{
							castle.setOwnerId(ownerId);
							
							// Schedule owner tasks to start running
							ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000);
						}
					}
				}
				rs2.close();
				
				// Store the Castle object with filled data in the array.
				_castles.add(castle);
			}
			
			rs.close();
			statement.close();
			statement2.close();
			
			_log.info("CastleManager: Loaded " + _castles.size() + " castles.");
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: loadCastleData(): " + e.getMessage(), e);
		}
	}
	
	public final Castle getCastleById(int castleId)
	{
		for (Castle castle : _castles)
		{
			if (castle.getCastleId() == castleId)
				return castle;
		}
		return null;
	}
	
	public final Castle getCastleByOwner(L2Clan clan)
	{
		for (Castle castle : _castles)
		{
			if (castle.getOwnerId() == clan.getClanId())
				return castle;
		}
		return null;
	}
	
	public final Castle getCastle(String name)
	{
		for (Castle castle : _castles)
		{
			if (castle.getName().equalsIgnoreCase(name.trim()))
				return castle;
		}
		return null;
	}
	
	public final Castle getCastle(int x, int y, int z)
	{
		for (Castle castle : _castles)
		{
			if (castle.checkIfInZone(x, y, z))
				return castle;
		}
		return null;
	}
	
	public final Castle getCastle(L2Object activeObject)
	{
		return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public final int getCastleIndex(int castleId)
	{
		Castle castle;
		for (int i = 0; i < _castles.size(); i++)
		{
			castle = _castles.get(i);
			if (castle != null && castle.getCastleId() == castleId)
				return i;
		}
		return -1;
	}
	
	public final int getCastleIndex(L2Object activeObject)
	{
		return getCastleIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public final int getCastleIndex(int x, int y, int z)
	{
		Castle castle;
		for (int i = 0; i < _castles.size(); i++)
		{
			castle = _castles.get(i);
			if (castle != null && castle.checkIfInZone(x, y, z))
				return i;
		}
		return -1;
	}
	
	public final List<Castle> getCastles()
	{
		return _castles;
	}
	
	public final void validateTaxes(int sealStrifeOwner)
	{
		int maxTax;
		switch (sealStrifeOwner)
		{
			case SevenSigns.CABAL_DUSK:
				maxTax = 5;
				break;
			case SevenSigns.CABAL_DAWN:
				maxTax = 25;
				break;
			default: // no owner
				maxTax = 15;
				break;
		}
		
		for (Castle castle : _castles)
		{
			if (castle.getTaxPercent() > maxTax)
				castle.setTaxPercent(maxTax, true);
		}
	}
	
	public int getCircletByCastleId(int castleId)
	{
		if (castleId > 0 && castleId < 10)
			return _castleCirclets[castleId];
		
		return 0;
	}
	
	// remove this castle's circlets from the clan
	public void removeCirclet(L2Clan clan, int castleId)
	{
		for (L2ClanMember member : clan.getMembers())
			removeCircletsAndCrown(member, castleId);
	}
	
	public void removeCircletsAndCrown(L2ClanMember member, int castleId)
	{
		if (member == null)
			return;
		
		L2PcInstance player = member.getPlayerInstance();
		int circletId = getCircletByCastleId(castleId);
		
		// online player actions
		if (player != null)
		{
			// Circlets removal for all members
			ItemInstance circlet = player.getInventory().getItemByItemId(circletId);
			if (circlet != null)
			{
				if (circlet.isEquipped())
					player.getInventory().unEquipItemInSlot(circlet.getLocationSlot());
				
				player.destroyItemByItemId("CastleCircletRemoval", circletId, 1, player, true);
			}
			
			// If the actual checked player is the clan leader, check for crown
			if (player.isClanLeader())
			{
				ItemInstance crown = player.getInventory().getItemByItemId(6841);
				if (crown != null)
				{
					if (crown.isEquipped())
						player.getInventory().unEquipItemInSlot(crown.getLocationSlot());
					
					player.destroyItemByItemId("CastleCrownRemoval", 6841, 1, player, true);
				}
			}
			return;
		}
		
		// offline player actions ; remove all circlets / crowns
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id IN (?, 6841)");
			statement.setInt(1, member.getObjectId());
			statement.setInt(2, circletId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Failed to remove castle circlets && crowns for offline player " + member.getName() + ": " + e.getMessage(), e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final CastleManager _instance = new CastleManager();
	}
}