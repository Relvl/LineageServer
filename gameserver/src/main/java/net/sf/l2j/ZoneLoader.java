package net.sf.l2j;

import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;

/**
 * @author Johnson / 17.07.2017
 */
public class ZoneLoader {

    public static void main(String... args) {
        L2DatabaseFactory.config = GameServer.CONFIG.load().database;
        ZoneManager.getInstance();
    }
}
