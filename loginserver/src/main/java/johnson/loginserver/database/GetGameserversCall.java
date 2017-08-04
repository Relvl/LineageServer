package johnson.loginserver.database;

import johnson.loginserver.GameServerInfo;
import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 02.06.2017
 */
public class GetGameserversCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetGameserversCall.class);

    /**  */
    @OrmParamOut(value = 1, cursorClass = GameServerInfo.class)
    private List<GameServerInfo> servers = new ArrayList<>();

    public GetGameserversCall() {
        super("GetGameServers", 0, true);
    }

    public List<GameServerInfo> getServers() {
        return servers;
    }

    @Override
    public String toString() {
        return "GetGameserversCall{" +
                "servers=" + servers +
                '}';
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
