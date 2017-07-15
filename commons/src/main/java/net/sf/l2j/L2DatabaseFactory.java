package net.sf.l2j;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import net.sf.l2j.commons.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class L2DatabaseFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(L2DatabaseFactory.class);
    public static DatabaseConfig config = new DatabaseConfig();

    private ComboPooledDataSource _source;

    public L2DatabaseFactory() throws SQLException {
        try {
            _source = new ComboPooledDataSource();
            _source.setAutoCommitOnClose(true);

            _source.setInitialPoolSize(10);
            _source.setMinPoolSize(10);
            _source.setMaxPoolSize(Math.max(10, config.maxConnections));

            // try to obtain connections indefinitely (0 = never quit)
            _source.setAcquireRetryAttempts(0);
            // 500 miliseconds wait before try to acquire connection again
            _source.setAcquireRetryDelay(500);
            // 0 = wait indefinitely for new connection
            _source.setCheckoutTimeout(0);
            // if pool is exhausted, get 5 more connections at a time cause there is a "long" delay on acquire connection so taking more than one connection at once will make connection pooling more effective.
            _source.setAcquireIncrement(5);

            // this "connection_test_table" is automatically created if not already there
            _source.setAutomaticTestTable("connection_test_table");
            // testing OnCheckin used with IdleConnectionTestPeriod is faster than testing on checkout
            _source.setTestConnectionOnCheckin(false);

            _source.setIdleConnectionTestPeriod(3600); // test idle connection every 60 sec
            _source.setMaxIdleTime(config.maxIdleTime); // 0 = idle connections never expire
            // *THANKS* to connection testing configured above but I prefer to disconnect all connections not used for more than 1 hour

            // enables statement caching, there is a "semi-bug" in c3p0 0.9.0 but in 0.9.0.2 and later it's fixed
            _source.setMaxStatementsPerConnection(100);

            // never fail if any way possible setting this to true will make c3p0 "crash" and refuse to work till restart
            // thus making acquire errors "FATAL" ... we don't want that it should be possible to recover
            _source.setBreakAfterAcquireFailure(false);

            _source.setDriverClass(config.driver);
            _source.setJdbcUrl(config.url);
            _source.setUser(config.user);
            _source.setPassword(config.password);

			/* Test the connection */
            _source.getConnection().close();
        }
        catch (SQLException x) {
            throw x;
        }
        catch (Exception e) {
            throw new SQLException("could not init DB connection:" + e);
        }
    }

    public static L2DatabaseFactory getInstance() {
        return SingletonHolder._instance;
    }

    public void shutdown() {
        try {
            _source.close();
            _source = null;
        }
        catch (Exception e) {
            LOGGER.error("Cannot close data source", e);
        }
    }

    public Connection getConnection() {
        Connection con = null;
        while (con == null) {
            try {
                con = _source.getConnection();
            }
            catch (SQLException e) {
                LOGGER.warn("L2DatabaseFactory: getConnection() failed, trying again ", e);
            }
        }
        return con;
    }

    private static class SingletonHolder {
        protected static final L2DatabaseFactory _instance;

        static {
            try {
                _instance = new L2DatabaseFactory();
            }
            catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }
}