package net.sf.l2j.loginserver;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.config.ExProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

@Deprecated
public class Config {
    public static final String LOGIN_CONFIGURATION_FILE = "./config/loginserver.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    public static boolean DEBUG = false;
    public static boolean DEVELOPER = false;

    public static String DATABASE_URL;
    public static String DATABASE_LOGIN;
    public static String DATABASE_PASSWORD;
    public static int DATABASE_MAX_CONNECTIONS;
    public static int DATABASE_MAX_IDLE_TIME;

    public static boolean FLOOD_PROTECTION;
    public static int FAST_CONNECTION_LIMIT;
    public static int NORMAL_CONNECTION_TIME;
    public static int FAST_CONNECTION_TIME;
    public static int MAX_CONNECTION_PER_IP;

    public static String GAME_SERVER_LOGIN_HOST;
    public static int GAME_SERVER_LOGIN_PORT;

    public static boolean AUTO_CREATE_ACCOUNTS;
    public static boolean LOG_LOGIN_CONTROLLER;
    public static int LOGIN_TRY_BEFORE_BAN;
    public static int LOGIN_BLOCK_AFTER_BAN;

    public static String LOGIN_BIND_ADDRESS;
    public static int PORT_LOGIN;

    public static boolean SHOW_LICENCE;

    public static int MMO_SELECTOR_SLEEP_TIME = 20; // default 20
    public static int MMO_MAX_SEND_PER_PASS = 12; // default 12
    public static int MMO_MAX_READ_PER_PASS = 12; // default 12
    public static int MMO_HELPER_BUFFER_COUNT = 20; // default 20

    public static boolean ACCEPT_NEW_GAMESERVER;

    public static void load() {
        LOGGER.info("Loading loginserver configuration files.");

        ExProperties server = load(LOGIN_CONFIGURATION_FILE);
        GAME_SERVER_LOGIN_HOST = server.getProperty("LoginHostname", "*");
        GAME_SERVER_LOGIN_PORT = server.getProperty("LoginPort", 9013);

        LOGIN_BIND_ADDRESS = server.getProperty("LoginserverHostname", "*");
        PORT_LOGIN = server.getProperty("LoginserverPort", 2106);

        DEBUG = server.getProperty("Debug", false);
        DEVELOPER = server.getProperty("Developer", false);
        ACCEPT_NEW_GAMESERVER = server.getProperty("AcceptNewGameServer", true);

        LOGIN_TRY_BEFORE_BAN = server.getProperty("LoginTryBeforeBan", 10);
        LOGIN_BLOCK_AFTER_BAN = server.getProperty("LoginBlockAfterBan", 600);

        LOG_LOGIN_CONTROLLER = server.getProperty("LogLoginController", false);

        DATABASE_URL = server.getProperty("URL", "jdbc:mysql://localhost/acis");
        DATABASE_LOGIN = server.getProperty("Login", "root");
        DATABASE_PASSWORD = server.getProperty("Password", "");
        DATABASE_MAX_CONNECTIONS = server.getProperty("MaximumDbConnections", 10);
        DATABASE_MAX_IDLE_TIME = server.getProperty("MaximumDbIdleTime", 0);

        SHOW_LICENCE = server.getProperty("ShowLicence", true);

        AUTO_CREATE_ACCOUNTS = server.getProperty("AutoCreateAccounts", true);

        FLOOD_PROTECTION = server.getProperty("EnableFloodProtection", true);
        FAST_CONNECTION_LIMIT = server.getProperty("FastConnectionLimit", 15);
        NORMAL_CONNECTION_TIME = server.getProperty("NormalConnectionTime", 700);
        FAST_CONNECTION_TIME = server.getProperty("FastConnectionTime", 350);
        MAX_CONNECTION_PER_IP = server.getProperty("MaxConnectionPerIP", 50);

        L2DatabaseFactory.DATABASE_URL = DATABASE_URL;
        L2DatabaseFactory.DATABASE_LOGIN = DATABASE_LOGIN;
        L2DatabaseFactory.DATABASE_PASSWORD = DATABASE_PASSWORD;
        L2DatabaseFactory.DATABASE_MAX_CONNECTIONS = DATABASE_MAX_CONNECTIONS;
        L2DatabaseFactory.DATABASE_MAX_IDLE_TIME = DATABASE_MAX_IDLE_TIME;
    }

    public static ExProperties load(String filename) {
        return load(new File(filename));
    }

    public static ExProperties load(File file) {
        ExProperties result = new ExProperties();

        try {
            result.load(file);
        } catch (IOException e) {
            LOGGER.warn("Error loading config : " + file.getName() + "!");
        }

        return result;
    }
}
