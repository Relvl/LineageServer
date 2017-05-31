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
package net.sf.l2j.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.instancemanager.AuctionManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.type.L2ClanHallZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class ClanHall
{
	protected static final Logger _log = Logger.getLogger(ClanHall.class.getName());
	
	private static final int CH_RATE = 604800000;
	
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_ITEM_CREATE = 2;
	public static final int FUNC_RESTORE_HP = 3;
	public static final int FUNC_RESTORE_MP = 4;
	public static final int FUNC_RESTORE_EXP = 5;
	public static final int FUNC_SUPPORT = 6;
	public static final int FUNC_DECO_FRONTPLATEFORM = 7;
	public static final int FUNC_DECO_CURTAINS = 8;
	
	private final int _id;
	private final String _name;
	private final int _lease;
	private final String _desc;
	private final String _location;
	private final int _grade;
	private final List<L2DoorInstance> _doors = new ArrayList<>();
	private final Map<Integer, ClanHallFunction> _functions = new ConcurrentHashMap<>();
	
	private int _ownerId;
	private L2ClanHallZone _zone;
	
	protected long _paidUntil;
	protected boolean _paid;
	protected boolean _isFree = true;
	
	public class ClanHallFunction
	{
		private final int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		private final long _rate;
		private long _endDate;
		protected boolean _inDebt;
		public boolean _cwh; // first activating clanhall function is paid from player inventory, any others from clan warehouse
		
		public ClanHallFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			
			initializeTask(cwh);
		}
		
		public int getType()
		{
			return _type;
		}
		
		public int getLvl()
		{
			return _lvl;
		}
		
		public void setLvl(int lvl)
		{
			_lvl = lvl;
		}
		
		public int getLease()
		{
			return _fee;
		}
		
		public void setLease(int lease)
		{
			_fee = lease;
		}
		
		public long getRate()
		{
			return _rate;
		}
		
		public long getEndTime()
		{
			return _endDate;
		}
		
		public void setEndTime(long time)
		{
			_endDate = time;
		}
		
		private void initializeTask(boolean cwh)
		{
			if (_isFree)
				return;
			
			long currentTime = System.currentTimeMillis();
			if (_endDate > currentTime)
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(cwh), _endDate - currentTime);
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(cwh), 0);
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}
			
			@Override
			public void run()
			{
				try
				{
					if (_isFree)
						return;
					
					final L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
					if (clan.getWarehouse().getAdena() >= _fee || !_cwh)
					{
						int fee = (getEndTime() == -1) ? _tempFee : _fee;
						
						setEndTime(System.currentTimeMillis() + getRate());
						dbSave();
						
						if (_cwh)
							clan.getWarehouse().destroyItemByItemId("CH_function_fee", 57, fee, null, null);
						
						ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(true), getRate());
					}
					else
						removeFunction(getType());
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "", e);
				}
			}
		}
		
		public void dbSave()
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("REPLACE INTO clanhall_functions (hall_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
				statement.setInt(1, getId());
				statement.setInt(2, getType());
				statement.setInt(3, getLvl());
				statement.setInt(4, getLease());
				statement.setLong(5, getRate());
				statement.setLong(6, getEndTime());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Exception: ClanHall.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
		}
	}
	
	public ClanHall(int id, String name, int ownerId, int lease, String desc, String location, long paidUntil, int grade, boolean paid)
	{
		_id = id;
		_name = name;
		_ownerId = ownerId;
		_lease = lease;
		_desc = desc;
		_location = location;
		_paidUntil = paidUntil;
		_grade = grade;
		_paid = paid;
		
		if (ownerId != 0)
		{
			_isFree = false;
			
			initializeTask(false);
			loadFunctions();
		}
	}
	
	/**
	 * @return if clanHall is paid or not
	 */
	public final boolean getPaid()
	{
		return _paid;
	}
	
	/**
	 * @return clanHall id
	 */
	public final int getId()
	{
		return _id;
	}
	
	/**
	 * @return clanHall name
	 */
	public final String getName()
	{
		return _name;
	}
	
	/**
	 * @return clanHall owner's id
	 */
	public final int getOwnerId()
	{
		return _ownerId;
	}
	
	/**
	 * @return clanHall lease
	 */
	public final int getLease()
	{
		return _lease;
	}
	
	/**
	 * @return clanHall description
	 */
	public final String getDesc()
	{
		return _desc;
	}
	
	/**
	 * @return clanHall location
	 */
	public final String getLocation()
	{
		return _location;
	}
	
	/**
	 * @return time that clanHall is paid on.
	 */
	public final long getPaidUntil()
	{
		return _paidUntil;
	}
	
	/**
	 * @return clanHall grade
	 */
	public final int getGrade()
	{
		return _grade;
	}
	
	/**
	 * @return clanHall doors list.
	 */
	public final List<L2DoorInstance> getDoors()
	{
		return _doors;
	}
	
	/**
	 * @param doorId The id to make checks on.
	 * @return a doorInstance based on a doorId.
	 */
	public final L2DoorInstance getDoor(int doorId)
	{
		for (L2DoorInstance door : _doors)
		{
			if (door.getDoorId() == doorId)
				return door;
		}
		return null;
	}
	
	/**
	 * @param type
	 * @return clanHall function with id
	 */
	public ClanHallFunction getFunction(int type)
	{
		return _functions.get(type);
	}
	
	/**
	 * Sets this clan halls zone
	 * @param zone
	 */
	public void setZone(L2ClanHallZone zone)
	{
		_zone = zone;
	}
	
	/**
	 * @return the zone of this clan hall
	 */
	public L2ClanHallZone getZone()
	{
		return _zone;
	}
	
	/** Free this clan hall */
	public void free()
	{
		_ownerId = 0;
		_isFree = true;
		_paidUntil = 0;
		_paid = false;
		
		removeAllFunctions();
		updateDb();
	}
	
	/**
	 * Set owner if clan hall is free
	 * @param clan The new clan owner.
	 */
	public void setOwner(L2Clan clan)
	{
		// Verify that this ClanHall is Free and Clan isn't null
		if (_ownerId > 0 || clan == null)
			return;
		
		_ownerId = clan.getClanId();
		_isFree = false;
		_paidUntil = System.currentTimeMillis();
		
		initializeTask(true);
		
		// Annonce to Online member new ClanHall
		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		updateDb();
	}
	
	/**
	 * Open or close a door.
	 * @param activeChar The player who request to open/close the door.
	 * @param doorId The affected doorId.
	 * @param open true will open it, false will close.
	 */
	public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
	{
		if (activeChar != null && activeChar.getClanId() == getOwnerId())
			openCloseDoor(doorId, open);
	}
	
	public void openCloseDoor(int doorId, boolean open)
	{
		openCloseDoor(getDoor(doorId), open);
	}
	
	public static void openCloseDoor(L2DoorInstance door, boolean open)
	{
		if (door != null)
		{
			if (open)
				door.openMe();
			else
				door.closeMe();
		}
	}
	
	public void openCloseDoors(L2PcInstance activeChar, boolean open)
	{
		if (activeChar != null && activeChar.getClanId() == getOwnerId())
			openCloseDoors(open);
	}
	
	public void openCloseDoors(boolean open)
	{
		for (L2DoorInstance door : _doors)
		{
			if (open)
				door.openMe();
			else
				door.closeMe();
		}
	}
	
	/** Banish Foreigner */
	public void banishForeigners()
	{
		_zone.banishForeigners(getOwnerId());
	}
	
	/** Load All Functions */
	private void loadFunctions()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("Select * from clanhall_functions where hall_id = ?");
			statement.setInt(1, getId());
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				_functions.put(rs.getInt("type"), new ClanHallFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: ClanHall.loadFunctions(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Remove function In List and in DB
	 * @param functionType the function id to remove.
	 */
	public void removeFunction(int functionType)
	{
		_functions.remove(functionType);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id=? AND type=?");
			statement.setInt(1, getId());
			statement.setInt(2, functionType);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: ClanHall.removeFunction(int functionType): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Remove all functions linked to a particular Clan Hall.
	 */
	public void removeAllFunctions()
	{
		_functions.clear();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id=?");
			statement.setInt(1, getId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: ClanHall.removeAllFunctions(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Update Function of a clanHall.
	 * @param player The player who requested the change.
	 * @param type
	 * @param lvl
	 * @param lease
	 * @param rate
	 * @param addNew
	 * @return
	 */
	public boolean updateFunctions(L2PcInstance player, int type, int lvl, int lease, long rate, boolean addNew)
	{
		if (player == null)
			return false;
		
		if (lease > 0)
		{
			if (!player.destroyItemByItemId("Consume", 57, lease, null, true))
				return false;
		}
		
		if (addNew)
			_functions.put(type, new ClanHallFunction(type, lvl, lease, 0, rate, 0, false));
		else
		{
			if (lvl == 0 && lease == 0)
				removeFunction(type);
			else
			{
				final ClanHallFunction chf = _functions.get(type);
				
				final int diffLease = lease - chf.getLease();
				if (diffLease > 0)
					_functions.put(type, new ClanHallFunction(type, lvl, lease, diffLease, rate, -1, false));
				else
				{
					chf.setLease(lease);
					chf.setLvl(lvl);
					chf.dbSave();
				}
			}
		}
		return true;
	}
	
	/** Update DB */
	public void updateDb()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clanhall SET ownerId=?, paidUntil=?, paid=? WHERE id=?");
			statement.setInt(1, _ownerId);
			statement.setLong(2, _paidUntil);
			statement.setInt(3, (_paid) ? 1 : 0);
			statement.setInt(4, _id);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initialize Fee Task
	 * @param forced
	 */
	private void initializeTask(boolean forced)
	{
		long currentTime = System.currentTimeMillis();
		if (_paidUntil > currentTime)
			ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - currentTime);
		else if (!_paid && !forced)
		{
			if (System.currentTimeMillis() + 86400000 <= _paidUntil + CH_RATE)
				ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), System.currentTimeMillis() + 86400000);
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), (_paidUntil + CH_RATE) - System.currentTimeMillis());
		}
		else
			ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), 0);
	}
	
	/** Fee Task */
	private class FeeTask implements Runnable
	{
		public FeeTask()
		{
		}
		
		@Override
		public void run()
		{
			try
			{
				long time = System.currentTimeMillis();
				
				if (_isFree)
					return;
				
				if (_paidUntil > time)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - time);
					return;
				}
				
				final L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
				if (clan.getWarehouse().getAdena() >= getLease())
				{
					if (_paidUntil != 0)
					{
						while (_paidUntil <= time)
							_paidUntil += CH_RATE;
					}
					else
						_paidUntil = time + CH_RATE;
					
					clan.getWarehouse().destroyItemByItemId("CH_rental_fee", 57, getLease(), null, null);
					
					ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - time);
					_paid = true;
					updateDb();
				}
				else
				{
					_paid = false;
					
					if (time > _paidUntil + CH_RATE)
					{
						if (ClanHallManager.getInstance().loaded())
						{
							AuctionManager.getInstance().initNPC(getId());
							ClanHallManager.getInstance().setFree(getId());
							clan.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
						}
						else
							ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), 3000);
					}
					else
					{
						updateDb();
						clan.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW).addNumber(getLease()));
						
						if (time + 86400000 <= _paidUntil + CH_RATE)
							ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), time + 86400000);
						else
							ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), (_paidUntil + CH_RATE) - time);
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
}