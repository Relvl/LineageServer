package johnson.loginserver.database;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 03.06.2017
 */
public class CreateAccountCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAccountCall.class);

    @OrmParamOut(1)
    private Integer accountId;
    @OrmParamIn(2)
    private final String pi_login;
    @OrmParamIn(3)
    private final String pi_password;

    public CreateAccountCall(String login, String password) {
        super("Create_Account", 2, true);
        this.pi_login = login;
        this.pi_password = password;
    }

    public Integer getAccountId() {
        return accountId;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
