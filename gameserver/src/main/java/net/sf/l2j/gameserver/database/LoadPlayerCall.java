package net.sf.l2j.gameserver.database;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 02.08.2017
 */
public class LoadPlayerCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadPlayerCall.class);

    @OrmParamIn(1)
    private final Integer playerId;
    @OrmParamOut(2)
    private UDT_Player player;
    @OrmParamOut(3)
    private Integer resultCode;

    public LoadPlayerCall(Integer playerId) {
        super("game_server.player__load", 3, false);
        this.playerId = playerId;
    }

    @Override
    public Integer getResultCode() {
        return resultCode;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String toString() {
        return "LoadPlayerCall{" +
                "playerId=" + playerId +
                ", player=" + player +
                ", resultCode=" + resultCode +
                '}';
    }
}
