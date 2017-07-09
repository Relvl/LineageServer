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
package net.sf.l2j.gameserver;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.datatables.*;
import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.handler.*;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.*;
import net.sf.l2j.gameserver.instancemanager.games.MonsterRace;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;
import net.sf.l2j.gameserver.model.vehicles.*;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GamePacketHandler;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.taskmanager.*;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import net.sf.l2j.util.DeadLockDetector;
import net.sf.l2j.util.IPv4Filter;
import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class GameServer {
    private static final Logger _log = Logger.getLogger(GameServer.class.getName());
    public static GameServer instance;

    private final SelectorThread<L2GameClient> _selectorThread;
    private final L2GamePacketHandler _gamePacketHandler;
    private final DeadLockDetector _deadDetectThread;
    private final LoginServerThread _loginThread;

    public GameServer() throws Exception {
        instance = this;

        IdFactory.getInstance();
        ThreadPoolManager.getInstance();

        new File("./data/crests").mkdirs();

        L2World.getInstance();
        MapRegionTable.getInstance();
        AnnouncementTable.getInstance();
        ServerMemo.getInstance();

        SkillTable.getInstance();
        SkillTreeTable.getInstance();

        ItemTable.getInstance();
        SummonItemsData.getInstance();
        BuyListTable.getInstance();
        MultisellData.getInstance();
        RecipeTable.getInstance();
        ArmorSetsTable.getInstance();
        FishTable.getInstance();
        SpellbookTable.getInstance();
        SoulCrystalsTable.load();
        AugmentationData.getInstance();
        CursedWeaponsManager.getInstance();

        AccessLevels.getInstance();
        AdminCommandAccessRights.getInstance();
        BookmarkTable.getInstance();
        GmListTable.getInstance();
        MovieMakerManager.getInstance();
        PetitionManager.getInstance();

        CharTemplateTable.getInstance();
        CharNameTable.getInstance();
        HennaTable.getInstance();
        HelperBuffTable.getInstance();
        TeleportLocationTable.getInstance();
        HtmCache.getInstance();
        PartyMatchWaitingList.getInstance();
        PartyMatchRoomList.getInstance();
        RaidBossPointsManager.getInstance();

        if (Config.ENABLE_COMMUNITY_BOARD) // Forums has to be loaded before clan data
        {
            ForumsBBSManager.getInstance().initRoot();
        }
        else {
            _log.config("Community server is disabled.");
        }

        CrestCache.getInstance();
        ClanTable.getInstance();
        AuctionManager.getInstance();
        ClanHallManager.getInstance();

        GeoData.initialize();
        PathFinding.initialize();

        GrandBossManager.getInstance();

        ZoneManager.getInstance();
        GrandBossManager.getInstance().initZones();

        AttackStanceTaskManager.getInstance();
        DecayTaskManager.getInstance();
        GameTimeTaskManager.getInstance();
        ItemsOnGroundTaskManager.getInstance();
        KnownListUpdateTaskManager.getInstance();
        MovementTaskManager.getInstance();
        PvpFlagTaskManager.getInstance();
        RandomAnimationTaskManager.getInstance();
        ShadowItemTaskManager.getInstance();
        WaterTaskManager.getInstance();

        CastleManager.getInstance().load();

        SevenSigns.getInstance().spawnSevenSignsNPC();
        SevenSignsFestival.getInstance();

        SiegeManager.getInstance();
        SiegeManager.getSieges();
        MercTicketManager.getInstance();

        CastleManorManager.getInstance();
        L2Manor.getInstance();

        BufferTable.getInstance();
        HerbDropTable.getInstance();
        PetDataTable.getInstance();
        NpcTable.getInstance();
        NpcWalkerRoutesTable.getInstance();
        DoorTable.getInstance();
        StaticObjects.load();
        SpawnTable.getInstance();
        RaidBossSpawnManager.getInstance();
        DayNightSpawnManager.getInstance();
        DimensionalRiftManager.getInstance();

        OlympiadGameManager.getInstance();
        Olympiad.getInstance();
        Hero.getInstance();

        FourSepulchersManager.getInstance().init();

        ScriptManager.getInstance();

        if (Config.ALLOW_BOAT) {
            BoatManager.getInstance();
            BoatGiranTalking.load();
            BoatGludinRune.load();
            BoatInnadrilTour.load();
            BoatRunePrimeval.load();
            BoatTalkingGludin.load();
        }

        MonsterRace.getInstance();

        _log.config("AutoSpawnHandler: Loaded " + AutoSpawnManager.getInstance().size() + " handlers.");
        _log.config("AdminCommandHandler: Loaded " + AdminCommandHandler.getInstance().size() + " handlers.");
        _log.config("ChatHandler: Loaded " + ChatHandler.getInstance().size() + " handlers.");
        _log.config("ItemHandler: Loaded " + ItemHandler.getInstance().size() + " handlers.");
        _log.config("SkillHandler: Loaded " + SkillHandler.getInstance().size() + " handlers.");
        _log.config("UserCommandHandler: Loaded " + UserCommandHandler.getInstance().size() + " handlers.");

        if (Config.ALLOW_WEDDING) {
            CoupleManager.getInstance();
        }

        if (Config.ALT_FISH_CHAMPIONSHIP_ENABLED) {
            FishingChampionshipManager.getInstance();
        }

        TaskManager.getInstance();
        Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
        ForumsBBSManager.getInstance();
        _log.config("IdFactory: Free ObjectIDs remaining: " + IdFactory.getInstance().size());

        if (Config.DEADLOCK_DETECTOR) {
            _log.info("Deadlock detector is enabled. Timer: " + Config.DEADLOCK_CHECK_INTERVAL + "s.");
            _deadDetectThread = new DeadLockDetector();
            _deadDetectThread.setDaemon(true);
            _deadDetectThread.start();
        }
        else {
            _log.info("Deadlock detector is disabled.");
            _deadDetectThread = null;
        }

        System.gc();

        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        long totalMem = Runtime.getRuntime().maxMemory() / 1048576;

        _log.info("Gameserver have started, used memory: " + usedMem + " / " + totalMem + " Mo.");
        _log.info("Maximum allowed players: " + Config.MAXIMUM_ONLINE_USERS);

        _loginThread = LoginServerThread.getInstance();
        _loginThread.start();

        final SelectorConfig sc = new SelectorConfig();
        sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
        sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
        sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
        sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;

        _gamePacketHandler = new L2GamePacketHandler();
        _selectorThread = new SelectorThread<>(sc, _gamePacketHandler, _gamePacketHandler, _gamePacketHandler, new IPv4Filter());

        InetAddress bindAddress = null;
        if (!Config.GAMESERVER_HOSTNAME.equals("*")) {
            try {
                bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
            } catch (UnknownHostException e1) {
                _log.log(Level.SEVERE, "WARNING: The GameServer bind address is invalid, using all available IPs. Reason: " + e1.getMessage(), e1);
            }
        }

        try {
            _selectorThread.openServerSocket(bindAddress, Config.PORT_GAME);
        } catch (IOException e) {
            _log.log(Level.SEVERE, "FATAL: Failed to open server socket. Reason: " + e.getMessage(), e);
            System.exit(1);
        }
        _selectorThread.start();
    }

    public static void main(String[] args) throws Exception {
        Server.serverMode = Server.MODE_GAMESERVER;

        final String LOG_FOLDER = "./log"; // Name of folder for log file
        final String LOG_NAME = "config/log.cfg"; // Name of log file

        // Create log folder
        File logFolder = new File(LOG_FOLDER);
        logFolder.mkdir();

        // Create input stream for log file -- or store file data into memory
        InputStream is = new FileInputStream(new File(LOG_NAME));
        LogManager.getLogManager().readConfiguration(is);
        is.close();

        // Initialize config
        Config.load();

        // Factories
        XMLDocumentFactory.getInstance();
        L2DatabaseFactory.getInstance();

        instance = new GameServer();
    }

    public long getUsedMemoryMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576; // 1024 * 1024 = 1048576;
    }

    public SelectorThread<L2GameClient> getSelectorThread() {
        return _selectorThread;
    }
}