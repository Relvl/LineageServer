package net.sf.l2j.gameserver.model.olympiad;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.Broadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

public class Olympiad {
    private static final Logger LOGGER = LoggerFactory.getLogger(Olympiad.class);

    private static final Map<Integer, StatsSet> _nobles = new HashMap<>();
    private static final Map<Integer, Integer> _noblesRank = new HashMap<>();

    protected static final List<StatsSet> _heroesToBe = new ArrayList<>();

    public static final String OLYMPIAD_HTML_PATH = "data/html/olympiad/";

    private static final String OLYMPIAD_LOAD_DATA = "SELECT current_cycle, period, olympiad_end, validation_end, next_weekly_change FROM olympiad_data WHERE id = 0";
    private static final String OLYMPIAD_SAVE_DATA = "INSERT INTO olympiad_data (id, current_cycle, period, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?) ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, validation_end=?, next_weekly_change=?";

    private static final String OLYMPIAD_LOAD_NOBLES = "SELECT olympiad_nobles.char_id, olympiad_nobles.class_id, characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn FROM olympiad_nobles, characters WHERE characters.obj_Id = olympiad_nobles.char_id";
    private static final String OLYMPIAD_SAVE_NOBLES = "INSERT INTO olympiad_nobles (`char_id`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`, `competitions_drawn`) VALUES (?,?,?,?,?,?,?)";
    private static final String OLYMPIAD_UPDATE_NOBLES = "UPDATE olympiad_nobles SET olympiad_points = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ? WHERE char_id = ?";
    private static final String OLYMPIAD_GET_HEROS = "SELECT olympiad_nobles.char_id, characters.char_name FROM olympiad_nobles, characters WHERE characters.obj_Id = olympiad_nobles.char_id AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " AND olympiad_nobles.competitions_won > 0 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC";
    private static final String GET_ALL_CLASSIFIED_NOBLESS = "SELECT char_id from olympiad_nobles_eom WHERE competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC";
    private static final String GET_EACH_CLASS_LEADER = "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.obj_Id = olympiad_nobles_eom.char_id AND olympiad_nobles_eom.class_id = ? AND olympiad_nobles_eom.competitions_done >= " + Config.ALT_OLY_MIN_MATCHES + " ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";

    private static final String OLYMPIAD_DELETE_ALL = "TRUNCATE olympiad_nobles";
    private static final String OLYMPIAD_MONTH_CLEAR = "TRUNCATE olympiad_nobles_eom";
    private static final String OLYMPIAD_MONTH_CREATE = "INSERT INTO olympiad_nobles_eom SELECT char_id, class_id, olympiad_points, competitions_done, competitions_won, competitions_lost, competitions_drawn FROM olympiad_nobles";

    private static final int COMP_START = Config.ALT_OLY_START_TIME; // 6PM
    private static final int COMP_MIN = Config.ALT_OLY_MIN; // 00 mins
    private static final long COMP_PERIOD = Config.ALT_OLY_CPERIOD; // 6 hours
    protected static final long WEEKLY_PERIOD = Config.ALT_OLY_WPERIOD; // 1 week
    protected static final long VALIDATION_PERIOD = Config.ALT_OLY_VPERIOD; // 24 hours

    protected static final int DEFAULT_POINTS = Config.ALT_OLY_START_POINTS;
    protected static final int WEEKLY_POINTS = Config.ALT_OLY_WEEKLY_POINTS;

    public static final String CHAR_ID = "char_id";
    public static final String CLASS_ID = "class_id";
    public static final String CHAR_NAME = "char_name";
    public static final String POINTS = "olympiad_points";
    public static final String COMP_DONE = "competitions_done";
    public static final String COMP_WON = "competitions_won";
    public static final String COMP_LOST = "competitions_lost";
    public static final String COMP_DRAWN = "competitions_drawn";

    protected long _olympiadEnd;
    protected long _validationEnd;

    /**
     * The current period of the olympiad.<br>
     * <b>0 -</b> Competition period<br>
     * <b>1 -</b> Validation Period
     */
    protected int _period;
    protected long _nextWeeklyChange;
    protected int _currentCycle;
    private long _compEnd;
    private Calendar _compStart;
    protected static boolean _inCompPeriod;
    protected static boolean _compStarted;

    protected ScheduledFuture<?> _scheduledCompStart;
    protected ScheduledFuture<?> _scheduledCompEnd;
    protected ScheduledFuture<?> _scheduledOlympiadEnd;
    protected ScheduledFuture<?> _scheduledWeeklyTask;
    protected ScheduledFuture<?> _scheduledValdationTask;
    protected ScheduledFuture<?> _gameManager;
    protected ScheduledFuture<?> _gameAnnouncer;

    public static Olympiad getInstance() {
        return SingletonHolder._instance;
    }

    protected Olympiad() {
        load();

        if (_period == 0) { init(); }
    }

    private void load() {
        boolean loaded = false;
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_DATA);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                _currentCycle = rset.getInt("current_cycle");
                _period = rset.getInt("period");
                _olympiadEnd = rset.getLong("olympiad_end");
                _validationEnd = rset.getLong("validation_end");
                _nextWeeklyChange = rset.getLong("next_weekly_change");
                loaded = true;
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Olympiad: Error loading olympiad data from database: ", e);
        }

        if (!loaded) {
            LOGGER.info("Olympiad: failed to load data from database, default values are used.");

            _currentCycle = 1;
            _period = 0;
            _olympiadEnd = 0;
            _validationEnd = 0;
            _nextWeeklyChange = 0;
        }

        switch (_period) {
            case 0:
                if (_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis()) { setNewOlympiadEnd(); }
                else { scheduleWeeklyChange(); }
                break;
            case 1:
                if (_validationEnd > Calendar.getInstance().getTimeInMillis()) {
                    loadNoblesRank();
                    _scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationEndTask(), getMillisToValidationEnd());
                }
                else {
                    _currentCycle++;
                    _period = 0;
                    deleteNobles();
                    setNewOlympiadEnd();
                }
                break;
            default:
                LOGGER.warn("Olympiad: something went wrong loading period: " + _period);
                return;
        }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
            ResultSet rset = statement.executeQuery();
            StatsSet statData;

            while (rset.next()) {
                statData = new StatsSet();
                statData.set(CLASS_ID, rset.getInt(CLASS_ID));
                statData.set(CHAR_NAME, rset.getString(CHAR_NAME));
                statData.set(POINTS, rset.getInt(POINTS));
                statData.set(COMP_DONE, rset.getInt(COMP_DONE));
                statData.set(COMP_WON, rset.getInt(COMP_WON));
                statData.set(COMP_LOST, rset.getInt(COMP_LOST));
                statData.set(COMP_DRAWN, rset.getInt(COMP_DRAWN));
                statData.set("to_save", false);

                addNobleStats(rset.getInt(CHAR_ID), statData);
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Olympiad: Error loading noblesse data from database: ", e);
        }

        synchronized (this) {
            if (_period == 0) {
                LOGGER.info("Olympiad: Currently in Competition period.");
            }
            else {
                LOGGER.info("Olympiad: Currently in Validation period.");
            }

            long milliToEnd;
            if (_period == 0) { milliToEnd = getMillisToOlympiadEnd(); }
            else { milliToEnd = getMillisToValidationEnd(); }

            LOGGER.info("Olympiad: " + Math.round(milliToEnd / 60000) + " minutes until period ends.");

            if (_period == 0) {
                milliToEnd = getMillisToWeekChange();
                LOGGER.info("Olympiad: Next weekly change is in " + Math.round(milliToEnd / 60000) + " minutes.");
            }
        }

        LOGGER.info("Olympiad: Loaded " + _nobles.size() + " nobles.");
    }

    public void loadNoblesRank() {
        _noblesRank.clear();

        Map<Integer, Integer> tmpPlace = new HashMap<>();

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(GET_ALL_CLASSIFIED_NOBLESS);
            ResultSet rset = statement.executeQuery();

            int place = 1;
            while (rset.next()) { tmpPlace.put(rset.getInt(CHAR_ID), place++); }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Olympiad: Error loading noblesse data from database for Ranking: ", e);
        }

        int rank1 = (int) Math.round(tmpPlace.size() * 0.01);
        int rank2 = (int) Math.round(tmpPlace.size() * 0.10);
        int rank3 = (int) Math.round(tmpPlace.size() * 0.25);
        int rank4 = (int) Math.round(tmpPlace.size() * 0.50);

        if (rank1 == 0) {
            rank1 = 1;
            rank2++;
            rank3++;
            rank4++;
        }

        for (int charId : tmpPlace.keySet()) {
            if (tmpPlace.get(charId) <= rank1) { _noblesRank.put(charId, 1); }
            else if (tmpPlace.get(charId) <= rank2) { _noblesRank.put(charId, 2); }
            else if (tmpPlace.get(charId) <= rank3) { _noblesRank.put(charId, 3); }
            else if (tmpPlace.get(charId) <= rank4) { _noblesRank.put(charId, 4); }
            else { _noblesRank.put(charId, 5); }
        }
    }

    protected void init() {
        if (_period == 1) { return; }

        _compStart = Calendar.getInstance();
        _compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
        _compStart.set(Calendar.MINUTE, COMP_MIN);
        _compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;

        if (_scheduledOlympiadEnd != null) { _scheduledOlympiadEnd.cancel(true); }

        _scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new OlympiadEndTask(), getMillisToOlympiadEnd());

        updateCompStatus();
    }

    protected class OlympiadEndTask implements Runnable {
        @Override
        public void run() {
            Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED).addNumber(_currentCycle));

            if (_scheduledWeeklyTask != null) { _scheduledWeeklyTask.cancel(true); }

            saveNobleData();

            _period = 1;
            sortHeroesToBe();
            Hero.getInstance().resetData();
            Hero.getInstance().computeNewHeroes(_heroesToBe);

            saveOlympiadStatus();
            updateMonthlyData();

            Calendar validationEnd = Calendar.getInstance();
            _validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERIOD;

            loadNoblesRank();
            _scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationEndTask(), getMillisToValidationEnd());
        }
    }

    protected class ValidationEndTask implements Runnable {
        @Override
        public void run() {
            _period = 0;
            _currentCycle++;

            deleteNobles();
            setNewOlympiadEnd();
            init();
        }
    }

    protected static int getNobleCount() {
        return _nobles.size();
    }

    protected static StatsSet getNobleStats(int playerId) {
        return _nobles.get(playerId);
    }

    private void updateCompStatus() {
        synchronized (this) {
            long milliToStart = getMillisToCompBegin();

            double numSecs = (milliToStart / 1000) % 60;
            double countDown = ((milliToStart / 1000) - numSecs) / 60;
            int numMins = (int) Math.floor(countDown % 60);
            countDown = (countDown - numMins) / 60;
            int numHours = (int) Math.floor(countDown % 24);
            int numDays = (int) Math.floor((countDown - numHours) / 24);

            LOGGER.info("Olympiad: Competition period starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
            LOGGER.info("Olympiad: Event starts/started : " + _compStart.getTime());
        }

        _scheduledCompStart = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
            @Override
            public void run() {
                if (isOlympiadEnd()) { return; }

                _inCompPeriod = true;

                Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_STARTED));
                LOGGER.info("Olympiad: Olympiad game started.");

                _gameManager = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(OlympiadGameManager.getInstance(), 30000, 30000);
                if (Config.ALT_OLY_ANNOUNCE_GAMES) { _gameAnnouncer = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new OlympiadAnnouncer(), 30000, 500); }

                long regEnd = getMillisToCompEnd() - 600000;
                if (regEnd > 0) {
                    ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
                        @Override
                        public void run() {
                            Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_REGISTRATION_PERIOD_ENDED));
                        }
                    }, regEnd);
                }

                _scheduledCompEnd = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
                    @Override
                    public void run() {
                        if (isOlympiadEnd()) { return; }

                        _inCompPeriod = false;
                        Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_ENDED));
                        LOGGER.info("Olympiad: Olympiad game ended.");

                        while (OlympiadGameManager.getInstance().isBattleStarted()) // cleared in game manager
                        {
                            // wait 1 minutes for end of pendings games
                            try {
                                Thread.sleep(60000);
                            }
                            catch (InterruptedException e) {
                            }
                        }

                        if (_gameManager != null) {
                            _gameManager.cancel(false);
                            _gameManager = null;
                        }

                        if (_gameAnnouncer != null) {
                            _gameAnnouncer.cancel(false);
                            _gameAnnouncer = null;
                        }

                        saveOlympiadStatus();

                        init();
                    }
                }, getMillisToCompEnd());
            }
        }, getMillisToCompBegin());
    }

    private long getMillisToOlympiadEnd() {
        return _olympiadEnd - Calendar.getInstance().getTimeInMillis();
    }

    public void manualSelectHeroes() {
        if (_scheduledOlympiadEnd != null) { _scheduledOlympiadEnd.cancel(true); }

        _scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new OlympiadEndTask(), 0);
    }

    protected long getMillisToValidationEnd() {
        if (_validationEnd > Calendar.getInstance().getTimeInMillis()) { return _validationEnd - Calendar.getInstance().getTimeInMillis(); }

        return 10L;
    }

    public boolean isOlympiadEnd() {
        return _period != 0;
    }

    protected void setNewOlympiadEnd() {
        Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_STARTED).addNumber(_currentCycle));

        Calendar currentTime = Calendar.getInstance();
        currentTime.add(Calendar.MONTH, 1);
        currentTime.set(Calendar.DAY_OF_MONTH, 1);
        currentTime.set(Calendar.AM_PM, Calendar.AM);
        currentTime.set(Calendar.HOUR, 12);
        currentTime.set(Calendar.MINUTE, 0);
        currentTime.set(Calendar.SECOND, 0);
        _olympiadEnd = currentTime.getTimeInMillis();

        Calendar nextChange = Calendar.getInstance();
        _nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
        scheduleWeeklyChange();
    }

    public boolean inCompPeriod() {
        return _inCompPeriod;
    }

    private long getMillisToCompBegin() {
        if (_compStart.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && _compEnd > Calendar.getInstance().getTimeInMillis()) { return 10L; }

        if (_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) { return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(); }

        return setNewCompBegin();
    }

    private long setNewCompBegin() {
        _compStart = Calendar.getInstance();
        _compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
        _compStart.set(Calendar.MINUTE, COMP_MIN);
        _compStart.add(Calendar.HOUR_OF_DAY, 24);
        _compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;

        LOGGER.info("Olympiad: New schedule @ " + _compStart.getTime());

        return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
    }

    protected long getMillisToCompEnd() {
        return _compEnd - Calendar.getInstance().getTimeInMillis();
    }

    private long getMillisToWeekChange() {
        if (_nextWeeklyChange > Calendar.getInstance().getTimeInMillis()) { return _nextWeeklyChange - Calendar.getInstance().getTimeInMillis(); }

        return 10L;
    }

    private void scheduleWeeklyChange() {
        _scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable() {
            @Override
            public void run() {
                addWeeklyPoints();
                LOGGER.info("Olympiad: Added weekly points to nobles.");

                Calendar nextChange = Calendar.getInstance();
                _nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
            }
        }, getMillisToWeekChange(), WEEKLY_PERIOD);
    }

    protected synchronized void addWeeklyPoints() {
        if (_period == 1) { return; }

        int currentPoints;
        for (StatsSet nobleInfo : _nobles.values()) {
            currentPoints = nobleInfo.getInteger(POINTS);
            currentPoints += WEEKLY_POINTS;
            nobleInfo.set(POINTS, currentPoints);
        }
    }

    public int getCurrentCycle() {
        return _currentCycle;
    }

    public boolean playerInStadia(L2PcInstance player) {
        return ZoneManager.getOlympiadStadium(player) != null;
    }

    /**
     * Save noblesse data to database
     */
    protected synchronized void saveNobleData() {
        if (_nobles == null || _nobles.isEmpty()) { return; }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement;
            for (Entry<Integer, StatsSet> nobleEntry : _nobles.entrySet()) {
                StatsSet nobleInfo = nobleEntry.getValue();
                if (nobleInfo == null) { continue; }

                int charId = nobleEntry.getKey();
                int classId = nobleInfo.getInteger(CLASS_ID);
                int points = nobleInfo.getInteger(POINTS);
                int compDone = nobleInfo.getInteger(COMP_DONE);
                int compWon = nobleInfo.getInteger(COMP_WON);
                int compLost = nobleInfo.getInteger(COMP_LOST);
                int compDrawn = nobleInfo.getInteger(COMP_DRAWN);
                boolean toSave = nobleInfo.getBool("to_save");

                if (toSave) {
                    statement = con.prepareStatement(OLYMPIAD_SAVE_NOBLES);
                    statement.setInt(1, charId);
                    statement.setInt(2, classId);
                    statement.setInt(3, points);
                    statement.setInt(4, compDone);
                    statement.setInt(5, compWon);
                    statement.setInt(6, compLost);
                    statement.setInt(7, compDrawn);

                    nobleInfo.set("to_save", false);
                }
                else {
                    statement = con.prepareStatement(OLYMPIAD_UPDATE_NOBLES);
                    statement.setInt(1, points);
                    statement.setInt(2, compDone);
                    statement.setInt(3, compWon);
                    statement.setInt(4, compLost);
                    statement.setInt(5, compDrawn);
                    statement.setInt(6, charId);
                }
                statement.execute();
                statement.close();
            }
        }
        catch (SQLException e) {
            LOGGER.error("Olympiad: Failed to save noblesse data to database: ", e);
        }
    }

    /**
     * Save current olympiad status and update noblesse table in database
     */
    public void saveOlympiadStatus() {
        saveNobleData();

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(OLYMPIAD_SAVE_DATA);

            statement.setInt(1, _currentCycle);
            statement.setInt(2, _period);
            statement.setLong(3, _olympiadEnd);
            statement.setLong(4, _validationEnd);
            statement.setLong(5, _nextWeeklyChange);
            statement.setInt(6, _currentCycle);
            statement.setInt(7, _period);
            statement.setLong(8, _olympiadEnd);
            statement.setLong(9, _validationEnd);
            statement.setLong(10, _nextWeeklyChange);

            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Olympiad: Failed to save olympiad data to database: ", e);
        }
    }

    protected void updateMonthlyData() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(OLYMPIAD_MONTH_CLEAR);
            statement.execute();
            statement.close();
            statement = con.prepareStatement(OLYMPIAD_MONTH_CREATE);
            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Olympiad: Failed to update monthly noblese data: ", e);
        }
    }

    protected void sortHeroesToBe() {
        _heroesToBe.clear();

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(OLYMPIAD_GET_HEROS);
            for (ClassId id : ClassId.values()) {
                if (id.level() != 3) { continue; }

                statement.setInt(1, id.getId());
                ResultSet rset = statement.executeQuery();
                statement.clearParameters();

                if (rset.next()) {
                    StatsSet hero = new StatsSet();
                    hero.set(CLASS_ID, id.getId());
                    hero.set(CHAR_ID, rset.getInt(CHAR_ID));
                    hero.set(CHAR_NAME, rset.getString(CHAR_NAME));

                    _heroesToBe.add(hero);
                }
                rset.close();
            }
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Olympiad: Couldnt load heroes to be from DB", e);
        }
    }

    public List<String> getClassLeaderBoard(int classId) {
        List<String> names = new ArrayList<>();
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(GET_EACH_CLASS_LEADER);
            statement.setInt(1, classId);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) { names.add(rset.getString(CHAR_NAME)); }

            statement.close();
            rset.close();
        }
        catch (SQLException e) {
            LOGGER.error("Olympiad: Couldn't load olympiad leaders from DB!", e);
        }
        return names;
    }

    public int getNoblessePasses(L2PcInstance player, boolean clear) {
        if ((player == null) || (_period != 1) || _noblesRank.isEmpty()) { return 0; }

        int objId = player.getObjectId();
        if (!_noblesRank.containsKey(objId)) { return 0; }

        StatsSet noble = _nobles.get(objId);
        if ((noble == null) || (noble.getInteger(POINTS) == 0)) { return 0; }

        int rank = _noblesRank.get(objId);
        int points = player.isHero() ? Config.ALT_OLY_HERO_POINTS : 0;
        switch (rank) {
            case 1:
                points += Config.ALT_OLY_RANK1_POINTS;
                break;
            case 2:
                points += Config.ALT_OLY_RANK2_POINTS;
                break;
            case 3:
                points += Config.ALT_OLY_RANK3_POINTS;
                break;
            case 4:
                points += Config.ALT_OLY_RANK4_POINTS;
                break;
            default:
                points += Config.ALT_OLY_RANK5_POINTS;
        }

        if (clear) { noble.set(POINTS, 0); }

        points *= Config.ALT_OLY_GP_PER_POINT;
        return points;
    }

    public int getNoblePoints(int objId) {
        if (_nobles == null || !_nobles.containsKey(objId)) { return 0; }

        return _nobles.get(objId).getInteger(POINTS);
    }

    public int getLastNobleOlympiadPoints(int objId) {
        int result = 0;
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT olympiad_points FROM olympiad_nobles_eom WHERE char_id = ?");
            statement.setInt(1, objId);
            ResultSet rs = statement.executeQuery();
            if (rs.first()) { result = rs.getInt(1); }
            rs.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not load last olympiad points:", e);
        }
        return result;
    }

    public int getCompetitionDone(int objId) {
        if (_nobles == null || !_nobles.containsKey(objId)) { return 0; }

        return _nobles.get(objId).getInteger(COMP_DONE);
    }

    public int getCompetitionWon(int objId) {
        if (_nobles == null || !_nobles.containsKey(objId)) { return 0; }

        return _nobles.get(objId).getInteger(COMP_WON);
    }

    public int getCompetitionLost(int objId) {
        if (_nobles == null || !_nobles.containsKey(objId)) { return 0; }

        return _nobles.get(objId).getInteger(COMP_LOST);
    }

    protected void deleteNobles() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(OLYMPIAD_DELETE_ALL);
            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("Olympiad: Couldn't delete nobles from DB!", e);
        }
        _nobles.clear();
    }

    /**
     * @param charId the noble object Id.
     * @param data   the stats set data to add.
     * @return the old stats set if the noble is already present, null otherwise.
     */
    protected static StatsSet addNobleStats(int charId, StatsSet data) {
        return _nobles.put(charId, data);
    }

    private static class SingletonHolder {
        protected static final Olympiad _instance = new Olympiad();
    }
}