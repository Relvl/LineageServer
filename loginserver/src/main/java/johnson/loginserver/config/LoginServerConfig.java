package johnson.loginserver.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import net.sf.l2j.commons.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Johnson / 31.05.2017
 */
@JacksonXmlRootElement(localName = "LoginServerConfig")
public class LoginServerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServerConfig.class);
    private static final File CONFIG_FILE = new File("./config/config.xml");
    private static final boolean SAVE_LOG_FILE_ON_ERROR = true;

    /**  */
    @JacksonXmlProperty(localName = "Protocol", isAttribute = true)
    public int protocolRevision = 0x0102;

    @JacksonXmlProperty(localName = "Database")
    public Database database = new Database();
    @JacksonXmlProperty(localName = "GameServerListener")
    public GameServerListener gameServerListener = new GameServerListener();
    @JacksonXmlProperty(localName = "ClientListener")
    public ClientListener clientListener = new ClientListener();
    @JacksonXmlProperty(localName = "FloodProtection")
    public FloodProtection floodProtection = new FloodProtection();
    @JacksonXmlProperty(localName = "MMOCore")
    public MMOCore mmoCore = new MMOCore();

    @JsonIgnore
    public static LoginServerConfig load() {
        try {
            return Serializer.MAPPER.readValue(CONFIG_FILE, LoginServerConfig.class);
        } catch (IOException e) {
            LOGGER.error("---------------------------------------");
            LOGGER.error("Failed to load config.xml", e);
            LOGGER.error("Loading default config...");
            LOGGER.error("---------------------------------------");
            LoginServerConfig config = new LoginServerConfig();
            if (SAVE_LOG_FILE_ON_ERROR) {
                config.save();
            }
            return config;
        } finally {
            LOGGER.info("Configuration loaded.");
        }
    }

    @JsonIgnore
    public void save() {
        try {
            LOGGER.info("Saving config.xml");
            Serializer.MAPPER.writeValue(CONFIG_FILE, this);
        } catch (IOException e) {
            LOGGER.error("---------------------------------------");
            LOGGER.error("Failed to save config.xml", e);
            LOGGER.error("---------------------------------------");
        }
    }

    /**  */
    public static class Database {
        /**  */
        @JacksonXmlProperty(localName = "Driver")
        public String driver = "org.postgresql.Driver";
        /**  */
        @JacksonXmlProperty(localName = "URL")
        public String url = "jdbc:postgresql://localhost:5432/newera_login";
        /**  */
        @JacksonXmlProperty(localName = "User")
        public String user = "postgres";
        /**  */
        @JacksonXmlProperty(localName = "Password")
        public String password = "postgres";
        /**  */
        @JacksonXmlProperty(localName = "MaxConnections")
        public Integer maxConnections = 10;
        /**  */
        @JacksonXmlProperty(localName = "MaxIdleTime")
        public Integer maxIdleTime = 0;
    }

    /**  */
    public static class GameServerListener {
        /**  */
        @JacksonXmlProperty(localName = "Host", isAttribute = true)
        public String host = "*";
        /**  */
        @JacksonXmlProperty(localName = "Port", isAttribute = true)
        public Integer port = 9013;
    }

    /**  */
    public static class ClientListener {
        /**  */
        @JacksonXmlProperty(localName = "Host", isAttribute = true)
        public String host = "*";
        /**  */
        @JacksonXmlProperty(localName = "Port", isAttribute = true)
        public Integer port = 2106;
        /**  */
        @JacksonXmlProperty(localName = "AutoCreateAccounts")
        public Boolean autoCreateAccounts = false;
        /**  */
        @JacksonXmlProperty(localName = "LoginsTryBeforeBan")
        public Integer loginsTryBeforeBan = 10;
        /**  */
        @JacksonXmlProperty(localName = "LoginsBlockAfterBan")
        public Integer loginsBlockAfterBan = 600;
        /**  */
        @JacksonXmlProperty(localName = "LoginTimeout")
        public Integer loginTimeout = 60000;
    }

    /**  */
    public static class FloodProtection {
        /**  */
        @JacksonXmlProperty(localName = "Enabled", isAttribute = true)
        public Boolean enabled = true;
        /**  */
        @JacksonXmlProperty(localName = "FastConnectionLimit")
        public Integer fastConnectionLimit = 15;
        /**  */
        @JacksonXmlProperty(localName = "FastConnectionTime")
        public Integer fastConnectionTime = 350;
        /**  */
        @JacksonXmlProperty(localName = "NormalConnectionTime")
        public Integer normalConnectionTime = 700;
        /**  */
        @JacksonXmlProperty(localName = "MaxConnectionsPerIP")
        public Integer maxConnectionsPerIP = 10;
    }

    /**  */
    public static class MMOCore {
        /**  */
        @JacksonXmlProperty(localName = "SelectorSleepTime")
        public Integer selectorSleepTime = 20;
        /**  */
        @JacksonXmlProperty(localName = "MaxSendPerPass")
        public Integer maxSendPerPass = 12;
        /**  */
        @JacksonXmlProperty(localName = "MaxReadPerPass")
        public Integer maxReadPerPass = 12;
        /**  */
        @JacksonXmlProperty(localName = "HelperBufferCount")
        public Integer helperBufferCount = 20;
    }
}
