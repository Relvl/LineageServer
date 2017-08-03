package net.sf.l2j.gameserver.communitybbs.BB;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.communitybbs.Manager.TopicBBSManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Topic {
    private static final Logger LOGGER = LoggerFactory.getLogger(Topic.class);

    public static final int MORMAL = 0;
    public static final int MEMO = 1;

    private final int _id;
    private final int _forumId;
    private final String _topicName;
    private final long _date;
    private final String _ownerName;
    private final int _ownerId;
    private final int _type;
    private final int _cReply;

    public Topic(ConstructorType ct, int id, int fid, String name, long date, String oname, int oid, int type, int Creply) {
        _id = id;
        _forumId = fid;
        _topicName = name;
        _date = date;
        _ownerName = oname;
        _ownerId = oid;
        _type = type;
        _cReply = Creply;
        TopicBBSManager.getInstance().addTopic(this);

        if (ct == ConstructorType.CREATE) { insertIntoDb(); }
    }

    private void insertIntoDb() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO topic (topic_id,topic_forum_id,topic_name,topic_date,topic_ownername,topic_ownerid,topic_type,topic_reply) VALUES (?,?,?,?,?,?,?,?)");
            statement.setInt(1, _id);
            statement.setInt(2, _forumId);
            statement.setString(3, _topicName);
            statement.setLong(4, _date);
            statement.setString(5, _ownerName);
            statement.setInt(6, _ownerId);
            statement.setInt(7, _type);
            statement.setInt(8, _cReply);
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error while saving new Topic to db " + e.getMessage(), e);
        }
    }

    public enum ConstructorType {
        RESTORE,
        CREATE
    }

    public int getID() {
        return _id;
    }

    public int getForumID() {
        return _forumId;
    }

    public String getName() {
        return _topicName;
    }

    public String getOwnerName() {
        return _ownerName;
    }

    public long getDate() {
        return _date;
    }

    public void deleteMe(Forum f) {
        TopicBBSManager.getInstance().delTopic(this);
        f.rmTopicByID(getID());

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM topic WHERE topic_id=? AND topic_forum_id=?");
            statement.setInt(1, getID());
            statement.setInt(2, f.getID());
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error while deleting topic: " + e.getMessage(), e);
        }
    }
}