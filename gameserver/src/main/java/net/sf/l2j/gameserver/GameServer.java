package net.sf.l2j.gameserver;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.Server;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.config.GameServerConfig;
import net.sf.l2j.gameserver.datatables.*;
import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.*;
import net.sf.l2j.gameserver.instancemanager.games.MonsterRace;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.recipe.RecipeController;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;
import net.sf.l2j.gameserver.model.vehicles.*;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GamePacketHandler;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.taskmanager.*;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import net.sf.l2j.util.DeadLockDetector;
import net.sf.l2j.util.IPv4Filter;
import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class GameServer {
    public static final GameServerConfig CONFIG = new GameServerConfig();
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServer.class);
    public static GameServer instance;
    private final SelectorThread<L2GameClient> _selectorThread;
    private final L2GamePacketHandler _gamePacketHandler;
    private final DeadLockDetector _deadDetectThread;
    private final LoginServerThread _loginThread;

    public GameServer() throws Exception {
        CONFIG.save(false);
        CONFIG.load();

        instance = this;

        L2DatabaseFactoryOld.config = CONFIG.database;
        L2DatabaseFactoryOld.getInstance();
        L2DatabaseFactory.config = CONFIG.database_new;
        L2DatabaseFactory.getInstance();

        IdFactory.getInstance();
        ThreadPoolManager.getInstance();

        new File("./data/crests").mkdirs();

        L2World.getInstance();
        MapRegionTable.getInstance();
        AnnouncementTable.getInstance();

        SkillTable.getInstance();
        SkillTreeTable.getInstance();

        ItemTable.getInstance();
        SummonItemsData.getInstance();
        BuyListTable.getInstance();
        MultisellData.getInstance();
        RecipeController.loadRecipes();
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

        ForumsBBSManager.getInstance().initRoot();

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

        BoatManager.getInstance();
        BoatGiranTalking.load();
        BoatGludinRune.load();
        BoatInnadrilTour.load();
        BoatRunePrimeval.load();
        BoatTalkingGludin.load();

        MonsterRace.getInstance();

        CoupleManager.getInstance();
        FishingChampionshipManager.getInstance();

        TaskManager.getInstance();
        Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
        ForumsBBSManager.getInstance();
        LOGGER.info("IdFactory: Free ObjectIDs remaining: " + IdFactory.getInstance().size());

        LOGGER.info("Deadlock detector is enabled. Timer: " + Config.DEADLOCK_CHECK_INTERVAL + "s.");
        _deadDetectThread = new DeadLockDetector();
        _deadDetectThread.setDaemon(true);
        _deadDetectThread.start();

        System.gc();

        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        long totalMem = Runtime.getRuntime().maxMemory() / 1048576;

        LOGGER.info("Gameserver have started, used memory: " + usedMem + " / " + totalMem + " Mo.");
        LOGGER.info("Maximum allowed players: " + Config.MAXIMUM_ONLINE_USERS);

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
            }
            catch (UnknownHostException e1) {
                LOGGER.error("WARNING: The GameServer bind address is invalid, using all available IPs. Reason: " + e1.getMessage(), e1);
            }
        }

        try {
            _selectorThread.openServerSocket(bindAddress, Config.PORT_GAME);
        }
        catch (IOException e) {
            LOGGER.error("FATAL: Failed to open server socket. Reason: {}", e.getMessage(), e);
            System.exit(1);
        }
        _selectorThread.start();
    }

    public static void main(String[] args) throws Exception {
        Server.serverMode = Server.MODE_GAMESERVER;

        // Initialize config
        Config.load();

        // Factories
        XMLDocumentFactory.getInstance();

        instance = new GameServer();
    }

    public long getUsedMemoryMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576; // 1024 * 1024 = 1048576;
    }

    public SelectorThread<L2GameClient> getSelectorThread() {
        return _selectorThread;
    }
}