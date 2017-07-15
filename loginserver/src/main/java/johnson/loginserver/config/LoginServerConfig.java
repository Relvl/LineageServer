package johnson.loginserver.config;

import net.sf.l2j.commons.config.Config;
import net.sf.l2j.commons.config.ConfigElement;
import net.sf.l2j.commons.config.FloodProtectionConfig;
import net.sf.l2j.commons.config.NetworkConfig;

/**
 * @author Johnson / 15.07.2017
 */
public class LoginServerConfig extends Config {

    @ConfigElement(fileName = "./config/_login_server.xml")
    public LoginServerConfigImpl loginServer = new LoginServerConfigImpl();

    @ConfigElement(fileName = "./config/flood_protection.xml")
    public FloodProtectionConfig floodProtection = new FloodProtectionConfig();

    public LoginServerConfig() {
        network = new NetworkConfig("*", 2106);
    }

    @Override
    public String toString() {
        return "LoginServerConfig{" +
                "loginServer=" + loginServer +
                ", floodProtection=" + floodProtection +
                "network=" + network +
                ", database=" + database +
                ", mmocore=" + mmocore +
                '}';
    }
}
