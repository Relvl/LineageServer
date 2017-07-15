package johnson.loginserver;

import johnson.loginserver.config.LoginServerConfig;
import johnson.loginserver.security.SecurityController;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.SystemExitReason;
import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**  */
public class LoginServer {
    /** Конфигурация логин-сервера */
    public static final LoginServerConfig CONFIG = new LoginServerConfig();
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServer.class);
    private static LoginServer serverInstance;
    private GameServerListener gameServerListener;
    private SelectorThread<L2LoginClient> selectorThread;

    public LoginServer() throws Exception {
        CONFIG.load();

        L2DatabaseFactory.config = CONFIG.database;
        L2DatabaseFactory.getInstance();

        SecurityController.getInstance();
        GameServerTable.getInstance();
        LoginController.getInstance();

        InetAddress bindAddress = null;
        if (!"*".equals(CONFIG.network.host)) {
            try {
                bindAddress = InetAddress.getByName(CONFIG.network.host);
            }
            catch (UnknownHostException e1) {
                LOGGER.error("The LoginServer bind address is invalid, using all available IPs.", e1);
            }
        }

        SelectorConfig selectorConfig = new SelectorConfig();
        selectorConfig.MAX_READ_PER_PASS = CONFIG.mmocore.maxReadPerPass;
        selectorConfig.MAX_SEND_PER_PASS = CONFIG.mmocore.maxSendPerPass;
        selectorConfig.SLEEP_TIME = CONFIG.mmocore.selectorSleepTime;
        selectorConfig.HELPER_BUFFER_COUNT = CONFIG.mmocore.helperBufferCount;

        L2LoginPacketHandler lph = new L2LoginPacketHandler();
        SelectorHelper selectorHelper = new SelectorHelper();
        try {
            selectorThread = new SelectorThread<>(selectorConfig, selectorHelper, lph, selectorHelper, selectorHelper);
        }
        catch (IOException e) {
            LOGGER.error("FATAL: Failed to open selector.", e);
            SystemExitReason.ERROR.perform();
        }

        try {
            gameServerListener = new GameServerListener();
            gameServerListener.start();
            LOGGER.info("Listening for gameservers on {}:{}", CONFIG.network.communicationHost, CONFIG.network.communicationPort);
        }
        catch (IOException e) {
            LOGGER.error("FATAL: Failed to start the gameserver listener.", e);
            SystemExitReason.ERROR.perform();
        }

        try {
            selectorThread.openServerSocket(bindAddress, CONFIG.network.port);
        }
        catch (IOException e) {
            LOGGER.error("FATAL: Failed to open server socket.", e);
            SystemExitReason.ERROR.perform();
        }
        selectorThread.start();
        LOGGER.info("Loginserver ready on {}:{}", bindAddress == null ? "*" : bindAddress.getHostAddress(), CONFIG.network.port);
    }

    public static void main(String[] args) throws Exception {
        serverInstance = new LoginServer();
    }

    public static LoginServer getInstance() {
        return serverInstance;
    }

    public GameServerListener getGameServerListener() {
        return gameServerListener;
    }

    public void shutdown(boolean restart) {
        Runtime.getRuntime().exit(restart ? SystemExitReason.RESTART.getCode() : SystemExitReason.SHUTDOWN.getCode());
    }
}