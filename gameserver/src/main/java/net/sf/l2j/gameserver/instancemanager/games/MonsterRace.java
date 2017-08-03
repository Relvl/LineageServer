package net.sf.l2j.gameserver.instancemanager.games;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.zone.type.L2DerbyTrackZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.DeleteObject;
import net.sf.l2j.gameserver.network.client.game_to_client.MonRaceInfo;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MonsterRace {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonsterRace.class);

    public static enum RaceState {
        ACCEPTING_BETS,
        WAITING,
        STARTING_RACE,
        RACE_END
    }

    protected static final PlaySound _sound1 = new PlaySound(1, "S_Race", 0, 0, 0, 0, 0);
    protected static final PlaySound _sound2 = new PlaySound(0, "ItemSound2.race_start", 1, 121209259, 12125, 182487, -3559);

    protected static final List<Integer> _npcTemplates = new ArrayList<>(); // List holding npc templates, shuffled on a new race.
    protected static final List<HistoryInfo> _history = new ArrayList<>(); // List holding old race records.
    protected static final Map<Integer, Long> _betsPerLane = new ConcurrentHashMap<>(); // Map holding all bets for each lane ; values setted to 0 after every race.
    protected static final List<Double> _odds = new ArrayList<>(); // List holding sorted odds per lane ; cleared at new odds calculation.

    protected static final int[][] _codes =
            {
                    {
                            -1,
                            0
                    },
                    {
                            0,
                            15322
                    },
                    {
                            13765,
                            -1
                    }
            };

    protected static int _raceNumber = 1;
    protected static int _finalCountdown = 0;
    protected static RaceState _state = RaceState.RACE_END;

    protected static MonRaceInfo _packet;

    private final L2Npc[] _monsters;
    private Constructor<?> _constructor;
    private int[][] _speeds;
    private final int[] _first, _second;

    protected MonsterRace() {
        // Feed _history with previous race results.
        loadHistory();

        // Feed _betsPerLane with stored informations on bets.
        loadBets();

        // Feed _npcTemplates, we will only have to shuffle it when needed.
        for (int i = 31003; i < 31027; i++) { _npcTemplates.add(i); }

        _monsters = new L2Npc[8];
        _speeds = new int[8][20];
        _first = new int[2];
        _second = new int[2];

        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Announcement(), 0, 1000);
    }

    public static MonsterRace getInstance() {
        return SingletonHolder._instance;
    }

    public static class HistoryInfo {
        private final int _raceId;
        private int _first;
        private int _second;
        private double _oddRate;

        public HistoryInfo(int raceId, int first, int second, double oddRate) {
            _raceId = raceId;
            _first = first;
            _second = second;
            _oddRate = oddRate;
        }

        public int getRaceId() {
            return _raceId;
        }

        public int getFirst() {
            return _first;
        }

        public int getSecond() {
            return _second;
        }

        public double getOddRate() {
            return _oddRate;
        }

        public void setFirst(int first) {
            _first = first;
        }

        public void setSecond(int second) {
            _second = second;
        }

        public void setOddRate(double oddRate) {
            _oddRate = oddRate;
        }
    }

    private class Announcement implements Runnable {
        public Announcement() {
        }

        @Override
        public void run() {
            if (_finalCountdown > 1200) { _finalCountdown = 0; }

            switch (_finalCountdown) {
                case 0:
                    newRace();
                    newSpeeds();

                    _state = RaceState.ACCEPTING_BETS;
                    _packet = new MonRaceInfo(_codes[0][0], _codes[0][1], getMonsters(), getSpeeds());

                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, _packet, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber));
                    break;

                case 30: // 30 sec
                case 60: // 1 min
                case 90: // 1 min 30 sec
                case 120: // 2 min
                case 150: // 2 min 30
                case 180: // 3 min
                case 210: // 3 min 30
                case 240: // 4 min
                case 270: // 4 min 30 sec
                case 330: // 5 min 30 sec
                case 360: // 6 min
                case 390: // 6 min 30 sec
                case 420: // 7 min
                case 450: // 7 min 30
                case 480: // 8 min
                case 510: // 8 min 30
                case 540: // 9 min
                case 570: // 9 min 30 sec
                case 630: // 10 min 30 sec
                case 660: // 11 min
                case 690: // 11 min 30 sec
                case 720: // 12 min
                case 750: // 12 min 30
                case 780: // 13 min
                case 810: // 13 min 30
                case 870: // 14 min 30 sec
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber));
                    break;

                case 300: // 5 min
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_STOP_IN_S1_MINUTES).addNumber(10));
                    break;

                case 600: // 10 min
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_STOP_IN_S1_MINUTES).addNumber(5));
                    break;

                case 840: // 14 min
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_STOP_IN_S1_MINUTES).addNumber(1));
                    break;

                case 900: // 15 min
                    _state = RaceState.WAITING;

                    calculateOdds();

                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE).addNumber(_raceNumber), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S1_TICKET_SALES_CLOSED));
                    break;

                case 960: // 16 min
                case 1020: // 17 min
                    final int minutes = (_finalCountdown == 960) ? 2 : 1;
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S2_BEGINS_IN_S1_MINUTES).addNumber(minutes));
                    break;

                case 1050: // 17 min 30 sec
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S1_BEGINS_IN_30_SECONDS));
                    break;

                case 1070: // 17 min 50 sec
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S1_COUNTDOWN_IN_FIVE_SECONDS));
                    break;

                case 1075: // 17 min 55 sec
                case 1076: // 17 min 56 sec
                case 1077: // 17 min 57 sec
                case 1078: // 17 min 58 sec
                case 1079: // 17 min 59 sec
                    final int seconds = 1080 - _finalCountdown;
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_BEGINS_IN_S1_SECONDS).addNumber(seconds));
                    break;

                case 1080: // 18 min
                    _state = RaceState.STARTING_RACE;
                    _packet = new MonRaceInfo(_codes[1][0], _codes[1][1], getMonsters(), getSpeeds());

                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_RACE_START), _sound1, _sound2, _packet);
                    break;

                case 1085: // 18 min 5 sec
                    _packet = new MonRaceInfo(_codes[2][0], _codes[2][1], getMonsters(), getSpeeds());

                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, _packet);
                    break;

                case 1115: // 18 min 35 sec
                    _state = RaceState.RACE_END;

                    // Populate history info with data, stores it in database.
                    final HistoryInfo info = _history.get(_history.size() - 1);
                    info.setFirst(getFirstPlace());
                    info.setSecond(getSecondPlace());
                    info.setOddRate(_odds.get(getFirstPlace() - 1));

                    saveHistory(info);
                    clearBets();

                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_FIRST_PLACE_S1_SECOND_S2).addNumber(getFirstPlace()).addNumber(getSecondPlace()), SystemMessage.getSystemMessage(SystemMessageId.MONSRACE_S1_RACE_END).addNumber(_raceNumber));
                    _raceNumber++;
                    break;

                case 1140: // 19 min
                    Broadcast.toAllPlayersInZoneType(L2DerbyTrackZone.class, new DeleteObject(getMonsters()[0]), new DeleteObject(getMonsters()[1]), new DeleteObject(getMonsters()[2]), new DeleteObject(getMonsters()[3]), new DeleteObject(getMonsters()[4]), new DeleteObject(getMonsters()[5]), new DeleteObject(getMonsters()[6]), new DeleteObject(getMonsters()[7]));
                    break;
            }
            _finalCountdown += 1;
        }
    }

    public void newRace() {
        // Edit _history.
        _history.add(new HistoryInfo(_raceNumber, 0, 0, 0));

        // Randomize _npcTemplates.
        Collections.shuffle(_npcTemplates);

        // Setup 8 new creatures ; pickup the first 8 from _npcTemplates.
        for (int i = 0; i < 8; i++) {
            try {
                NpcTemplate template = NpcTable.getInstance().getTemplate(_npcTemplates.get(i));
                _constructor = Class.forName("net.sf.l2j.gameserver.model.actor.instance." + template.getType() + "Instance").getConstructors()[0];
                int objectId = IdFactory.getInstance().getNextId();
                _monsters[i] = (L2Npc) _constructor.newInstance(objectId, template);
            }
            catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    public void newSpeeds() {
        _speeds = new int[8][20];
        int total = 0;
        _first[1] = 0;
        _second[1] = 0;

        for (int i = 0; i < 8; i++) {
            total = 0;
            for (int j = 0; j < 20; j++) {
                if (j == 19) { _speeds[i][j] = 100; }
                else { _speeds[i][j] = Rnd.get(60) + 65; }
                total += _speeds[i][j];
            }

            if (total >= _first[1]) {
                _second[0] = _first[0];
                _second[1] = _first[1];
                _first[0] = 8 - i;
                _first[1] = total;
            }
            else if (total >= _second[1]) {
                _second[0] = 8 - i;
                _second[1] = total;
            }
        }
    }

    /**
     * Load past races informations, feeding _history arrayList.<br>
     * Also sets _raceNumber, based on latest HistoryInfo loaded.
     */
    protected static void loadHistory() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM mdt_history");
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                _history.add(new HistoryInfo(rset.getInt("race_id"), rset.getInt("first"), rset.getInt("second"), rset.getDouble("odd_rate")));
                _raceNumber++;
            }
            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("MonsterRace: can't load history: {}", e.getMessage(), e);
        }
        LOGGER.info("MonsterRace : loaded {} records, currently on race #{}", _history.size(), _raceNumber);
    }

    /**
     * Save an history record into database.
     *
     * @param history The infos to store.
     */
    protected static void saveHistory(HistoryInfo history) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO mdt_history (race_id, first, second, odd_rate) VALUES (?,?,?,?)");
            statement.setInt(1, history.getRaceId());
            statement.setInt(2, history.getFirst());
            statement.setInt(3, history.getSecond());
            statement.setDouble(4, history.getOddRate());
            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("MonsterRace: can't save history: {}", e.getMessage(), e);
        }
    }

    /**
     * Load current bets per lane ; initialize the map keys.
     */
    protected static void loadBets() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM mdt_bets");
            ResultSet rset = statement.executeQuery();

            while (rset.next()) { setBetOnLane(rset.getInt("lane_id"), rset.getLong("bet"), false); }

            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("MonsterRace: can't load bets: {}", e.getMessage(), e);
        }
    }

    /**
     * Save the current lane bet into database.
     *
     * @param lane : The lane to affect.
     * @param sum  : The sum to set.
     */
    protected static void saveBet(int lane, long sum) {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("REPLACE INTO mdt_bets (lane_id, bet) VALUES (?,?)");
            statement.setInt(1, lane);
            statement.setLong(2, sum);
            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("MonsterRace: can't save bet: {}", e.getMessage(), e);
        }
    }

    /**
     * Clear all lanes bets, either on database or Map.
     */
    protected static void clearBets() {
        for (int key : _betsPerLane.keySet()) { _betsPerLane.put(key, 0L); }

        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE mdt_bets SET bet = 0");
            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.error("MonsterRace: can't clear bets: {}", e.getMessage(), e);
        }
    }

    /**
     * Setup lane bet, based on previous value (if any).
     *
     * @param lane     : The lane to edit.
     * @param amount   : The amount to add.
     * @param saveOnDb : Should it be saved on db or not.
     */
    public static void setBetOnLane(int lane, long amount, boolean saveOnDb) {
        final long sum = (_betsPerLane.containsKey(lane)) ? _betsPerLane.get(lane) + amount : amount;

        _betsPerLane.put(lane, sum);

        if (saveOnDb) { saveBet(lane, sum); }
    }

    /**
     * Calculate odds for every lane, based on others lanes.
     */
    protected static void calculateOdds() {
        // Clear previous List holding old odds.
        _odds.clear();

        // Sort bets lanes per lane.
        final Map<Integer, Long> sortedLanes = new TreeMap<>(_betsPerLane);

        // Pass a first loop in order to calculate total sum of all lanes.
        long sumOfAllLanes = 0;
        for (long amount : sortedLanes.values()) { sumOfAllLanes += amount; }

        // As we get the sum, we can now calculate the odd rate of each lane.
        for (long amount : sortedLanes.values()) { _odds.add((amount == 0) ? 0D : Math.max(1.25, sumOfAllLanes * 0.7 / amount)); }
    }

    public L2Npc[] getMonsters() {
        return _monsters;
    }

    public int[][] getSpeeds() {
        return _speeds;
    }

    public int getFirstPlace() {
        return _first[0];
    }

    public int getSecondPlace() {
        return _second[0];
    }

    public MonRaceInfo getRacePacket() {
        return _packet;
    }

    public RaceState getCurrentRaceState() {
        return _state;
    }

    public int getRaceNumber() {
        return _raceNumber;
    }

    public List<HistoryInfo> getHistory() {
        return _history;
    }

    public List<Double> getOdds() {
        return _odds;
    }

    private static class SingletonHolder {
        protected static final MonsterRace _instance = new MonsterRace();
    }
}