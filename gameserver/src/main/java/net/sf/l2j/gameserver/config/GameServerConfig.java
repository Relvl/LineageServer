package net.sf.l2j.gameserver.config;

import net.sf.l2j.commons.config.Config;
import net.sf.l2j.commons.config.ConfigElement;
import net.sf.l2j.commons.config.NetworkConfig;

/**
 * @author Johnson / 15.07.2017
 */
public class GameServerConfig extends Config {

    @ConfigElement(fileName = "./config/_game_server.xml")
    public GameServerConfigImpl gameServer = new GameServerConfigImpl();

    public GameServerConfig() {
        network = new NetworkConfig("*", 7777);
    }

    @Override
    public String toString() {
        return "GameServerConfig{" +
                "gameServer=" + gameServer +
                "network=" + network +
                ", database=" + database +
                ", mmocore=" + mmocore +
                '}';
    }
}
