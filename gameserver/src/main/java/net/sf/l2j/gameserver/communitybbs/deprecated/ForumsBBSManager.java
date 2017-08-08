package net.sf.l2j.gameserver.communitybbs.deprecated;

import net.sf.l2j.L2DatabaseFactoryOld;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ForumsBBSManager extends BaseBBSManager {
    private final List<Forum> _table;
    private int _lastid = 1;

    protected ForumsBBSManager() {
        _table = new CopyOnWriteArrayList<>();

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT forum_id FROM forums WHERE forum_type=0");
            ResultSet result = statement.executeQuery();

            while (result.next()) { addForum(new Forum(result.getInt("forum_id"), null)); }

            result.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Data error on Forum (root): {}", e.getMessage(), e);
        }
    }

    public static ForumsBBSManager getInstance() {
        return SingletonHolder._instance;
    }

    public void initRoot() {
        for (Forum f : _table) { f.vload(); }

        LOGGER.info("Loaded {} forums. Last forum id used: {}", _table.size(), _lastid);
    }

    public void addForum(Forum ff) {
        if (ff == null) { return; }

        _table.add(ff);

        if (ff.getID() > _lastid) { _lastid = ff.getID(); }
    }

    public Forum getForumByName(String Name) {
        for (Forum f : _table) {
            if (f.getName().equals(Name)) { return f; }
        }
        return null;
    }

    public Forum createNewForum(String name, Forum parent, int type, int perm, int oid) {
        Forum forum = new Forum(name, parent, type, perm, oid);
        forum.insertIntoDb();

        return forum;
    }

    public int getANewID() {
        return ++_lastid;
    }

    public Forum getForumByID(int id) {
        for (Forum f : _table) {
            if (f.getID() == id) { return f; }
        }
        return null;
    }

    private static class SingletonHolder {
        protected static final ForumsBBSManager _instance = new ForumsBBSManager();
    }
}