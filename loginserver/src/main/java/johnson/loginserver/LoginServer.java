package johnson.loginserver;

import johnson.loginserver.config.LoginServerConfig;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.SystemExitReason;
import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**  */
public class LoginServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServer.class);

    /** Конфигурация логин-сервера */
    public static LoginServerConfig config = LoginServerConfig.load();

    private static LoginServer serverInstance;
    private GameServerListener gameServerListener;
    private SelectorThread<L2LoginClient> selectorThread;

    public LoginServer() throws Exception {
        // Factories
        L2DatabaseFactory.DATABASE_DRIVER = config.database.driver;
        L2DatabaseFactory.DATABASE_URL = config.database.url;
        L2DatabaseFactory.DATABASE_LOGIN = config.database.user;
        L2DatabaseFactory.DATABASE_PASSWORD = config.database.password;
        L2DatabaseFactory.DATABASE_MAX_IDLE_TIME = config.database.maxIdleTime;
        L2DatabaseFactory.DATABASE_MAX_CONNECTIONS = config.database.maxConnections;
        L2DatabaseFactory.getInstance();

        LoginController.load();
        GameServerTable.getInstance();

        loadBanFile();

        InetAddress bindAddress = null;
        if (!"*".equals(config.clientListener.host)) {
            try {
                bindAddress = InetAddress.getByName(config.clientListener.host);
            } catch (UnknownHostException e1) {
                LOGGER.error("The LoginServer bind address is invalid, using all available IPs.", e1);
            }
        }

        final SelectorConfig selectorConfig = new SelectorConfig();
        selectorConfig.MAX_READ_PER_PASS = config.mmoCore.maxReadPerPass;
        selectorConfig.MAX_SEND_PER_PASS = config.mmoCore.maxSendPerPass;
        selectorConfig.SLEEP_TIME = config.mmoCore.selectorSleepTime;
        selectorConfig.HELPER_BUFFER_COUNT = config.mmoCore.helperBufferCount;

        final L2LoginPacketHandler lph = new L2LoginPacketHandler();
        final SelectorHelper selectorHelper = new SelectorHelper();
        try {
            selectorThread = new SelectorThread<>(selectorConfig, selectorHelper, lph, selectorHelper, selectorHelper);
        } catch (IOException e) {
            LOGGER.error("FATAL: Failed to open selector.", e);
            SystemExitReason.ERROR.perform();
        }

        try {
            gameServerListener = new GameServerListener();
            gameServerListener.start();
            LOGGER.info("Listening for gameservers on {}:{}", config.gameServerListener.host, config.gameServerListener.port);
        } catch (IOException e) {
            LOGGER.error("FATAL: Failed to start the gameserver listener.", e);
            SystemExitReason.ERROR.perform();
        }

        try {
            selectorThread.openServerSocket(bindAddress, config.clientListener.port);
        } catch (IOException e) {
            LOGGER.error("FATAL: Failed to open server socket.", e);
            SystemExitReason.ERROR.perform();
        }
        selectorThread.start();
        LOGGER.info("Loginserver ready on {}:{}", bindAddress == null ? "*" : bindAddress.getHostAddress(), config.clientListener.port);
    }

    public static void main(String[] args) throws Exception {
        serverInstance = new LoginServer();
    }

    public static LoginServer getInstance() {
        return serverInstance;
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
                                LOGGER.error("Skipped: Incorrect ban duration ({}) on banned_ip.cfg. Line: {}", parts[1], reader.getLineNumber());
                                continue;
                            }
                        }

                        try {
                            LoginController.getInstance().addBanForAddress(address, duration);
                        } catch (UnknownHostException e) {
                            LOGGER.error("Skipped: Invalid address ({}) on banned_ip.cfg. Line: {}", parts[0], reader.getLineNumber());
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error while reading banned_ip.cfg.");
            }
            LOGGER.info("Loaded {} IP(s) from banned_ip.cfg.", LoginController.getInstance().getBannedIps().size());
        }
        else {
            LOGGER.warn("banned_ip.cfg is missing. Ban listing is skipped.");
        }
    }

    public GameServerListener getGameServerListener() {
        return gameServerListener;
    }

    public void shutdown(boolean restart) {
        Runtime.getRuntime().exit(restart ? SystemExitReason.RESTART.getCode() : SystemExitReason.SHUTDOWN.getCode());
    }
}