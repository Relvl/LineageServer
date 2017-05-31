package net.sf.l2j.loginserver;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.commons.lang.StringUtil;
import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class L2LoginServer {
    public static final int PROTOCOL_REV = 0x0102;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(L2LoginServer.class);
    private static final Logger _log = Logger.getLogger(L2LoginServer.class.getName());
    private static L2LoginServer loginServer;

    private GameServerListener _gameServerListener;
    private SelectorThread<L2LoginClient> _selectorThread;

    public L2LoginServer() throws Exception {
        Server.serverMode = Server.MODE_LOGINSERVER;

        LOGGER.info("------ TEST OK");

        final String LOG_FOLDER = "./log"; // Name of folder for log file
        final String LOG_NAME = "config/log.cfg"; // Name of log file

        // Create log folder
        File logFolder = new File(LOG_FOLDER);
        logFolder.mkdir();

        // Create input stream for log file -- or store file data into memory
        InputStream is = new FileInputStream(new File(LOG_NAME));
        LogManager.getLogManager().readConfiguration(is);
        is.close();

        StringUtil.printSection("aCis");

        // Initialize config
        Config.load();

        // Factories
        L2DatabaseFactory.getInstance();

        StringUtil.printSection("LoginController");
        LoginController.load();
        GameServerTable.getInstance();

        StringUtil.printSection("Ban List");
        loadBanFile();

        StringUtil.printSection("IP, Ports & Socket infos");
        InetAddress bindAddress = null;
        if (!Config.LOGIN_BIND_ADDRESS.equals("*")) {
            try {
                bindAddress = InetAddress.getByName(Config.LOGIN_BIND_ADDRESS);
            } catch (UnknownHostException e1) {
                _log.severe("WARNING: The LoginServer bind address is invalid, using all available IPs. Reason: " + e1.getMessage());
                if (Config.DEVELOPER)
                    e1.printStackTrace();
            }
        }

        final SelectorConfig sc = new SelectorConfig();
        sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
        sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
        sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
        sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;

        final L2LoginPacketHandler lph = new L2LoginPacketHandler();
        final SelectorHelper sh = new SelectorHelper();
        try {
            _selectorThread = new SelectorThread<>(sc, sh, lph, sh, sh);
        } catch (IOException e) {
            _log.severe("FATAL: Failed to open selector. Reason: " + e.getMessage());
            if (Config.DEVELOPER)
                e.printStackTrace();

            System.exit(1);
        }

        try {
            _gameServerListener = new GameServerListener();
            _gameServerListener.start();
            _log.info("Listening for gameservers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
        } catch (IOException e) {
            _log.severe("FATAL: Failed to start the gameserver listener. Reason: " + e.getMessage());
            if (Config.DEVELOPER)
                e.printStackTrace();

            System.exit(1);
        }

        try {
            _selectorThread.openServerSocket(bindAddress, Config.PORT_LOGIN);
        } catch (IOException e) {
            _log.severe("FATAL: Failed to open server socket. Reason: " + e.getMessage());
            if (Config.DEVELOPER)
                e.printStackTrace();

            System.exit(1);
        }
        _selectorThread.start();
        _log.info("Loginserver ready on " + (bindAddress == null ? "*" : bindAddress.getHostAddress()) + ":" + Config.PORT_LOGIN);

        StringUtil.printSection("Waiting for gameserver answer");
    }

    public static void main(String[] args) throws Exception {
        loginServer = new L2LoginServer();
    }

    public static L2LoginServer getInstance() {
        return loginServer;
    }

    private static void loadBanFile() {
        File banFile = new File("config/banned_ip.cfg");
        if (banFile.exists() && banFile.isFile()) {
            try (LineNumberReader reader = new LineNumberReader(new FileReader(banFile))) {
                String line;
                String[] parts;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // check if this line isnt a comment line
                    if (line.length() > 0 && line.charAt(0) != '#') {
                        // split comments if any
                        parts = line.split("#");

                        // discard comments in the line, if any
                        line = parts[0];
                        parts = line.split(" ");

                        String address = parts[0];
                        long duration = 0;

                        if (parts.length > 1) {
                            try {
                                duration = Long.parseLong(parts[1]);
                            } catch (NumberFormatException e) {
                                _log.warning("Skipped: Incorrect ban duration (" + parts[1] + ") on banned_ip.cfg. Line: " + reader.getLineNumber());
                                continue;
                            }
                        }

                        try {
                            LoginController.getInstance().addBanForAddress(address, duration);
                        } catch (UnknownHostException e) {
                            _log.warning("Skipped: Invalid address (" + parts[0] + ") on banned_ip.cfg. Line: " + reader.getLineNumber());
                        }
                    }
                }
            } catch (IOException e) {
                _log.warning("Error while reading banned_ip.cfg. Details: " + e.getMessage());
                if (Config.DEVELOPER)
                    e.printStackTrace();
            }
            _log.info("Loaded " + LoginController.getInstance().getBannedIps().size() + " IP(s) from banned_ip.cfg.");
        } else
            _log.warning("banned_ip.cfg is missing. Ban listing is skipped.");
    }

    public GameServerListener getGameServerListener() {
        return _gameServerListener;
    }

    public void shutdown(boolean restart) {
        Runtime.getRuntime().exit(restart ? 2 : 0);
    }
}