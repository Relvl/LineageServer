package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class RaidBossPointsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaidBossPointsManager.class);

    private final Map<Integer, Map<Integer, Integer>> _list = new ConcurrentHashMap<>();

    private final Comparator<Entry<Integer, Integer>> _comparator = new Comparator<Entry<Integer, Integer>>() {
        @Override
        public int compare(Entry<Integer, Integer> entry, Entry<Integer, Integer> entry1) {
            return entry.getValue().equals(entry1.getValue()) ? 0 : entry.getValue() < entry1.getValue() ? 1 : -1;
        }
    };

    public static final RaidBossPointsManager getInstance() {
        return SingletonHolder._instance;
    }

    public RaidBossPointsManager() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT `char_id`,`boss_id`,`points` FROM `character_raid_points`");
            ResultSet rset = statement.executeQuery();
            while (rset.next()) {
                int charId = rset.getInt("char_id");
                int bossId = rset.getInt("boss_id");
                int points = rset.getInt("points");
                Map<Integer, Integer> values = _list.get(charId);
                if (values == null) { values = new HashMap<>(); }

                values.put(bossId, points);
                _list.put(charId, values);
            }
            rset.close();
            statement.close();
            LOGGER.info("{}: Loaded {} characters with Raid Points infos.", getClass().getSimpleName(), _list.size());
        }
        catch (SQLException e) {
            LOGGER.error("RaidPointsManager: Couldnt load Raid Points characters infos ", e);
        }
    }

    public static final void updatePointsInDB(L2PcInstance player, int raidId, int points) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("REPLACE INTO character_raid_points (`char_id`,`boss_id`,`points`) VALUES (?,?,?)");
            statement.setInt(1, player.getObjectId());
            statement.setInt(2, raidId);
            statement.setInt(3, points);
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("could not update char raid points:", e);
        }
    }

    public final void addPoints(L2PcInstance player, int bossId, int points) {
        int ownerId = player.getObjectId();
        Map<Integer, Integer> tmpPoint = _list.get(ownerId);
        if (tmpPoint == null) {
            tmpPoint = new HashMap<>();
            tmpPoint.put(bossId, points);
            updatePointsInDB(player, bossId, points);
        }
        else {
            int currentPoins = tmpPoint.containsKey(bossId) ? tmpPoint.get(bossId) : 0;
            currentPoins += points;
            tmpPoint.put(bossId, currentPoins);
            updatePointsInDB(player, bossId, currentPoins);
        }
        _list.put(ownerId, tmpPoint);
    }

    public final int getPointsByOwnerId(int ownerId) {
        Map<Integer, Integer> tmpPoint = _list.get(ownerId);
        if (tmpPoint == null || tmpPoint.isEmpty()) { return 0; }

        int totalPoints = 0;
        for (int points : tmpPoint.values()) { totalPoints += points; }

        return totalPoints;
    }

    public final Map<Integer, Integer> getList(L2PcInstance player) {
        return _list.get(player.getObjectId());
    }

    public final void cleanUp() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM character_raid_points WHERE char_id > 0");
            statement.executeUpdate();
            statement.close();
            _list.clear();
        }
        catch (Exception e) {
            LOGGER.error("could not clean raid points: ", e);
        }
    }

    public final int calculateRanking(int playerObjId) {
        Map<Integer, Integer> rank = getRankList();
        if (rank.containsKey(playerObjId)) { return rank.get(playerObjId); }

        return 0;
    }

    public Map<Integer, Integer> getRankList() {
        Map<Integer, Integer> tmpRanking = new HashMap<>();
        Map<Integer, Integer> tmpPoints = new HashMap<>();

        for (int ownerId : _list.keySet()) {
            int totalPoints = getPointsByOwnerId(ownerId);
            if (totalPoints != 0) { tmpPoints.put(ownerId, totalPoints); }
        }
        ArrayList<Entry<Integer, Integer>> list = new ArrayList<>(tmpPoints.entrySet());

        Collections.sort(list, _comparator);

        int ranking = 1;
        for (Entry<Integer, Integer> entry : list) { tmpRanking.put(entry.getKey(), ranking++); }

        return tmpRanking;
    }

    private static class SingletonHolder {
        protected static final RaidBossPointsManager _instance = new RaidBossPointsManager();
    }
}