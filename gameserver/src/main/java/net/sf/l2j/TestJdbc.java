package net.sf.l2j;

import net.sf.l2j.commons.database.CallException;
import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import net.sf.l2j.commons.database.annotation.OrmTypeName;
import net.sf.l2j.gameserver.config.GameServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 22.07.2017
 */
public class TestJdbc {
    public static final GameServerConfig CONFIG = new GameServerConfig();

    public static void main(String... args) {
        CONFIG.load();
        L2DatabaseFactory.config = CONFIG.database_new;
        L2DatabaseFactory.getInstance();

        try (TestCall call = new TestCall()) {
            call.execute();
        }
        catch (CallException e) {
            e.printStackTrace();
        }
    }

    public static class TestCall extends IndexedCall {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestCall.class);

        @OrmParamOut(1)
        private PlayerPojo out;

        protected TestCall() {
            super("game_server._test_func", 1, false);

        }

        @Override
        public Logger getLogger() {
            return LOGGER;
        }
    }

    @OrmTypeName("game_server.type_player")
    public static class PlayerPojo {
        private Integer objectid;
        private String name;
        private String title;
    }
}
