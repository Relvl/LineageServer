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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2Bookmark;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class BookmarkTable
{
	private static Logger _log = Logger.getLogger(BookmarkTable.class.getName());
	
	private final List<L2Bookmark> _bks;
	
	public static BookmarkTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected BookmarkTable()
	{
		_bks = new ArrayList<>();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM bookmarks");
			ResultSet result = statement.executeQuery();
			
			while (result.next())
				_bks.add(new L2Bookmark(result.getString("name"), result.getInt("obj_Id"), result.getInt("x"), result.getInt("y"), result.getInt("z")));
			
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "BookmarkTable: Error restoring BookmarkTable: ", e);
		}
		_log.info("BookmarkTable: Restored " + _bks.size() + " bookmarks.");
	}
	
	/**
	 * Verify if that location exists for that particular id.
	 * @param name The location name.
	 * @param objId The player Id to make checks on.
	 * @return true if the location exists, false otherwise.
	 */
	public boolean isExisting(String name, int objId)
	{
		return getBookmark(name, objId) != null;
	}
	
	/**
	 * Retrieve a bookmark by its name and its specific player.
	 * @param name The location name.
	 * @param objId The player Id to make checks on.
	 * @return null if such bookmark doesn't exist, the L2Bookmark otherwise.
	 */
	public L2Bookmark getBookmark(String name, int objId)
	{
		for (L2Bookmark bk : _bks)
		{
			if (bk.getName().equalsIgnoreCase(name) && bk.getId() == objId)
				return bk;
		}
		return null;
	}
	
	/**
	 * Retrieve the list of bookmarks of one player.
	 * @param objId The player Id to make checks on.
	 * @return an array of L2Bookmark.
	 */
	public List<L2Bookmark> getBookmarks(int objId)
	{
		return _bks.stream().filter(bk -> bk.getId() == objId).collect(Collectors.toList());
	}
	
	/**
	 * Creates a new bookmark and store info to database
	 * @param name The name of the bookmark.
	 * @param player The player who requested the clan creation.
	 */
	public void saveBookmark(String name, L2PcInstance player)
	{
		final int objId = player.getObjectId();
		final int x = player.getX();
		final int y = player.getY();
		final int z = player.getZ();
		
		_bks.add(new L2Bookmark(name, objId, x, y, z));
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO bookmarks (name, obj_Id, x, y, z) values (?,?,?,?,?)");
			statement.setString(1, name);
			statement.setInt(2, objId);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error adding bookmark on DB.", e);
		}
	}
	
	/**
	 * Delete a bookmark, based on the playerId and its name.
	 * @param name The name of the bookmark.
	 * @param objId The player Id to make checks on.
	 */
	public void deleteBookmark(String name, int objId)
	{
		final L2Bookmark bookmark = getBookmark(name, objId);
		if (bookmark != null)
		{
			_bks.remove(bookmark);
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("DELETE FROM bookmarks WHERE name=? AND obj_Id=?");
				statement.setString(1, name);
				statement.setInt(2, objId);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error removing bookmark from DB.", e);
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final BookmarkTable _instance = new BookmarkTable();
	}
}