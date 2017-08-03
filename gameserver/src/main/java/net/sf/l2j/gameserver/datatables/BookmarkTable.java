package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.L2Bookmark;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookmarkTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookmarkTable.class);
    private final List<L2Bookmark> _bks;

    public static BookmarkTable getInstance() {
        return SingletonHolder._instance;
    }

    protected BookmarkTable() {
        _bks = new ArrayList<>();

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM bookmarks");
            ResultSet result = statement.executeQuery();

            while (result.next()) { _bks.add(new L2Bookmark(result.getString("name"), result.getInt("obj_Id"), result.getInt("x"), result.getInt("y"), result.getInt("z"))); }

            result.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("BookmarkTable: Error restoring BookmarkTable: ", e);
        }
        LOGGER.info("BookmarkTable: Restored {} bookmarks.", _bks.size());
    }

    /**
     * Verify if that location exists for that particular id.
     *
     * @param name  The location name.
     * @param objId The player Id to make checks on.
     * @return true if the location exists, false otherwise.
     */
    public boolean isExisting(String name, int objId) {
        return getBookmark(name, objId) != null;
    }

    /**
     * Retrieve a bookmark by its name and its specific player.
     *
     * @param name  The location name.
     * @param objId The player Id to make checks on.
     * @return null if such bookmark doesn't exist, the L2Bookmark otherwise.
     */
    public L2Bookmark getBookmark(String name, int objId) {
        for (L2Bookmark bk : _bks) {
            if (bk.getName().equalsIgnoreCase(name) && bk.getId() == objId) { return bk; }
        }
        return null;
    }

    /**
     * Retrieve the list of bookmarks of one player.
     *
     * @param objId The player Id to make checks on.
     * @return an array of L2Bookmark.
     */
    public List<L2Bookmark> getBookmarks(int objId) {
        return _bks.stream().filter(bk -> bk.getId() == objId).collect(Collectors.toList());
    }

    /**
     * Creates a new bookmark and store info to database
     *
     * @param name   The name of the bookmark.
     * @param player The player who requested the clan creation.
     */
    public void saveBookmark(String name, L2PcInstance player) {
        int objId = player.getObjectId();
        int x = player.getX();
        int y = player.getY();
        int z = player.getZ();

        _bks.add(new L2Bookmark(name, objId, x, y, z));

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO bookmarks (name, obj_Id, x, y, z) VALUES (?,?,?,?,?)");
            statement.setString(1, name);
            statement.setInt(2, objId);
            statement.setInt(3, x);
            statement.setInt(4, y);
            statement.setInt(5, z);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error adding bookmark on DB.", e);
        }
    }

    /**
     * Delete a bookmark, based on the playerId and its name.
     *
     * @param name  The name of the bookmark.
     * @param objId The player Id to make checks on.
     */
    public void deleteBookmark(String name, int objId) {
        L2Bookmark bookmark = getBookmark(name, objId);
        if (bookmark != null) {
            _bks.remove(bookmark);

            try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement("DELETE FROM bookmarks WHERE name=? AND obj_Id=?");
                statement.setString(1, name);
                statement.setInt(2, objId);
                statement.execute();
                statement.close();
            }
            catch (Exception e) {
                LOGGER.error("Error removing bookmark from DB.", e);
            }
        }
    }

    private static class SingletonHolder {
        protected static final BookmarkTable _instance = new BookmarkTable();
    }
}