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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Couple;

/**
 * @author evill33t
 */
public class CoupleManager
{
	private static final Logger _log = Logger.getLogger(CoupleManager.class.getName());
	
	protected CoupleManager()
	{
		load();
	}
	
	public static final CoupleManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private List<Couple> _couples;
	
	public void reload()
	{
		_couples.clear();
		load();
	}
	
	private final void load()
	{
		_couples = new ArrayList<>();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT id FROM mods_wedding ORDER BY id");
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
				_couples.add(new Couple(rs.getInt("id")));
			
			rs.close();
			statement.close();
			
			_log.info("CoupleManager : Loaded " + getCouples().size() + " couples.");
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: CoupleManager.load(): " + e.getMessage(), e);
		}
	}
	
	public final Couple getCouple(int coupleId)
	{
		int index = getCoupleIndex(coupleId);
		if (index >= 0)
			return _couples.get(index);
		
		return null;
	}
	
	public void createCouple(L2PcInstance player1, L2PcInstance player2)
	{
		if (player1 != null && player2 != null)
		{
			Couple _new = new Couple(player1, player2);
			_couples.add(_new);
			player1.setCoupleId(_new.getId());
			player2.setCoupleId(_new.getId());
		}
	}
	
	public void deleteCouple(int coupleId)
	{
		int index = getCoupleIndex(coupleId);
		Couple couple = _couples.get(index);
		if (couple != null)
		{
			L2PcInstance player1 = L2World.getInstance().getPlayer(couple.getPlayer1Id());
			L2PcInstance player2 = L2World.getInstance().getPlayer(couple.getPlayer2Id());
			
			if (player1 != null)
			{
				player1.setMarried(false);
				player1.setCoupleId(0);
			}
			
			if (player2 != null)
			{
				player2.setMarried(false);
				player2.setCoupleId(0);
			}
			couple.divorce();
			_couples.remove(index);
		}
	}
	
	public final int getCoupleIndex(int coupleId)
	{
		int i = 0;
		for (Couple temp : _couples)
		{
			if (temp != null && temp.getId() == coupleId)
				return i;
			
			i++;
		}
		return -1;
	}
	
	public final List<Couple> getCouples()
	{
		return _couples;
	}
	
	private static class SingletonHolder
	{
		protected static final CoupleManager _instance = new CoupleManager();
	}
}