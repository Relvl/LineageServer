package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CharNameTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CharNameTable.class);

    private final Map<Integer, String> _chars;
    private final Map<Integer, Integer> _accessLevels;

    protected CharNameTable() {
        _chars = new HashMap<>();
        _accessLevels = new HashMap<>();
    }

    public static CharNameTable getInstance() {
        return SingletonHolder._instance;
    }

    public final void addName(L2PcInstance player) {
        if (player != null) {
            addName(player.getObjectId(), player.getName());
            _accessLevels.put(player.getObjectId(), player.getAccessLevel().getLevel());
        }
    }

    private final void addName(int objId, String name) {
        if (name != null) {
            if (!name.equalsIgnoreCase(_chars.get(objId))) { _chars.put(objId, name); }
        }
    }

    public final void removeName(int objId) {
        _chars.remove(objId);
        _accessLevels.remove(objId);
    }

    public final int getIdByName(String name) {
        if (name == null || name.isEmpty()) { return -1; }

        for (Map.Entry<Integer, String> entry : _chars.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) { return entry.getKey(); }
        }

        int id = -1;
        int accessLevel = 0;
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT obj_Id,accesslevel FROM characters WHERE char_name=?");
            statement.setString(1, name);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                id = rset.getInt(1);
                accessLevel = rset.getInt(2);
            }
            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Could not check existing char name: {}", e.getMessage(), e);
        }

        if (id > 0) {
            _chars.put(id, name);
            _accessLevels.put(id, accessLevel);
            return id;
        }

        return -1; // not found
    }

    public final String getNameById(int id) {
        if (id <= 0) { return null; }

        String name = _chars.get(id);
        if (name != null) { return name; }

        int accessLevel = 0;

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT char_name,accesslevel FROM characters WHERE obj_Id=?");
            statement.setInt(1, id);
            ResultSet rset = statement.executeQuery();
            while (rset.next()) {
                name = rset.getString(1);
                accessLevel = rset.getInt(2);
            }
            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Could not check existing char id: {}", e.getMessage(), e);
        }

        if (name != null && !name.isEmpty()) {
            _chars.put(id, name);
            _accessLevels.put(id, accessLevel);
            return name;
        }

        return null; // not found
    }

    public final int getAccessLevelById(int objectId) {
        if (getNameById(objectId) != null) { return _accessLevels.get(objectId); }

        return 0;
    }

    public synchronized static boolean doesCharNameExist(String name) {
        boolean result = true;
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
            statement.setString(1, name);
            ResultSet rset = statement.executeQuery();
            result = rset.next();
            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Could not check existing charname: {}", e.getMessage(), e);
        }
        return result;
    }

    public static int accountCharNumber(String account) {
        int number = 0;
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT COUNT(char_name) FROM characters WHERE account_name=?");
            statement.setString(1, account);
            ResultSet rset = statement.executeQuery();
            while (rset.next()) {
                number = rset.getInt(1);
            }
            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Could not check existing char number: {}", e.getMessage(), e);
        }
        return number;
    }

    private static class SingletonHolder {
        protected static final CharNameTable _instance = new CharNameTable();
    }
}