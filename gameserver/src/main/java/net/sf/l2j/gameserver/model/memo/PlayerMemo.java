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
package net.sf.l2j.gameserver.model.memo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;

/**
 * @author UnAfraid
 */
@SuppressWarnings("serial")
public class PlayerMemo extends AbstractMemo
{
	private static final Logger _log = Logger.getLogger(PlayerMemo.class.getName());
	
	private static final String SELECT_QUERY = "SELECT * FROM character_memo WHERE charId = ?";
	private static final String DELETE_QUERY = "DELETE FROM character_memo WHERE charId = ?";
	private static final String INSERT_QUERY = "INSERT INTO character_memo (charId, var, val) VALUES (?, ?, ?)";
	
	private final int _objectId;
	
	public PlayerMemo(int objectId)
	{
		_objectId = objectId;
		restoreMe();
	}
	
	@Override
	public boolean restoreMe()
	{
		// Restore previous variables.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement st = con.prepareStatement(SELECT_QUERY);
			st.setInt(1, _objectId);
			
			ResultSet rset = st.executeQuery();
			while (rset.next())
				set(rset.getString("var"), rset.getString("val"));
			
			rset.close();
			st.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't restore variables for player id: " + _objectId, e);
			return false;
		}
		finally
		{
			compareAndSetChanges(true, false);
		}
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
			PreparedStatement st = con.prepareStatement(DELETE_QUERY);
			st.setInt(1, _objectId);
			st.execute();
			st.close();
			
			// Insert all variables.
			st = con.prepareStatement(INSERT_QUERY);
			st.setInt(1, _objectId);
			for (Entry<String, Object> entry : entrySet())
			{
				st.setString(2, entry.getKey());
				st.setString(3, String.valueOf(entry.getValue()));
				st.addBatch();
			}
			st.executeBatch();
			st.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't update variables for player id: " + _objectId, e);
			return false;
		}
		finally
		{
			compareAndSetChanges(true, false);
		}
		return true;
	}
}