package johnson.loginserver.database;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 11.07.2017
 */
public class SetAccountLastServerCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetAccountLastServerCall.class);

    @OrmParamIn(1)
    private final String login;
    @OrmParamIn(2)
    private final Integer lastServer;
    @OrmParamOut(3)
    private Integer resultCode;

    public SetAccountLastServerCall(String login, Integer lastServer) {
        super("set_account_last_server", 2, true);
        this.login = login;
        this.lastServer = lastServer;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Integer getResultCode() {
        return resultCode;
    }

    @Override
    public String toString() {
        return "SetAccountLastServerCall{" +
                "login='" + login + '\'' +
                ", lastServer=" + lastServer +
                ", resultCode=" + resultCode +
                '}';
    }
}
