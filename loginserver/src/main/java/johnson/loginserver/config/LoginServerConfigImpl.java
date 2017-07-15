package johnson.loginserver.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Johnson / 15.07.2017
 */
public class LoginServerConfigImpl {
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

    @Override
    public String toString() {
        return "LoginServerConfigImpl{" +
                "autoCreateAccounts=" + autoCreateAccounts +
                ", loginsTryBeforeBan=" + loginsTryBeforeBan +
                ", loginsBlockAfterBan=" + loginsBlockAfterBan +
                ", loginTimeout=" + loginTimeout +
                '}';
    }
}
