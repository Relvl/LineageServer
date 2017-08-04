package johnson.loginserver.database;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 03.06.2017
 */
public class LoginCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginCall.class);

    @OrmParamIn(1)
    private final String pi_login;
    @OrmParamIn(2)
    private final String pi_password;
    @OrmParamOut(3)
    private Integer po_account_id;
    @OrmParamOut(4)
    private Integer po_last_server;

    public LoginCall(String login, String password) {
        super("Login", 4, false);
        this.pi_login = login;
        this.pi_password = password;
    }

    public Integer getAccountId() {
        return po_account_id;
    }

    public int getLastServer() {
        return po_last_server == null ? 0 : po_last_server;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
