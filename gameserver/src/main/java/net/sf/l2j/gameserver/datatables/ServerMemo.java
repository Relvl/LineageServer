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
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.memo.AbstractMemo;

@SuppressWarnings("serial")
public class ServerMemo extends AbstractMemo
{
	private static final Logger _log = Logger.getLogger(ServerMemo.class.getName());
	
	private static final String SELECT_QUERY = "SELECT * FROM server_memo";
	private static final String DELETE_QUERY = "DELETE FROM server_memo";
	private static final String INSERT_QUERY = "INSERT INTO server_memo (var, value) VALUES (?, ?)";
	
	protected ServerMemo()
	{
		restoreMe();
	}
	
	@Override
	public boolean restoreMe()
	{
		// Restore previous variables.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			Statement st = con.createStatement();
			
			ResultSet rset = st.executeQuery(SELECT_QUERY);
			while (rset.next())
				set(rset.getString("var"), rset.getString("value"));
			
			rset.close();
			st.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't restore global variables");
			return false;
		}
		finally
		{
			compareAndSetChanges(true, false);
		}
		_log.log(Level.INFO, getClass().getSimpleName() + ": Loaded " + size() + " variables.");
		return true;
	}
	
	@Override
	public boolean storeMe()
	{
		// No changes, nothing to store.
		if (!hasChanges())
			return false;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Clear previous entries.
			Statement del = con.createStatement();
			del.execute(DELETE_QUERY);
			del.close();
			
			// Insert all variables.
			PreparedStatement st = con.prepareStatement(INSERT_QUERY);
			for (Entry<String, Object> entry : entrySet())
			{
				st.setString(1, entry.getKey());
				st.setString(2, String.valueOf(entry.getValue()));
				st.addBatch();
			}
			st.executeBatch();
			st.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't save global variables to database.", e);
			return false;
		}
		finally
		{
			compareAndSetChanges(true, false);
		}
		_log.log(Level.INFO, getClass().getSimpleName() + ": Stored " + size() + " variables.");
		return true;
	}
	
	/**
	 * Gets the single instance of {@code ServerMemo}.
	 * @return single instance of {@code ServerMemo}
	 */
	public static final ServerMemo getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ServerMemo _instance = new ServerMemo();
	}
}