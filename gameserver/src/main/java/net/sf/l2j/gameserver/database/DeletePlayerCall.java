package net.sf.l2j.gameserver.database;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 04.08.2017
 */
public class DeletePlayerCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePlayerCall.class);

    @OrmParamOut(1)
    private Integer resultCode;
    @OrmParamIn(2)
    private final Integer playerId;

    public DeletePlayerCall(Integer playerId) {
        super("player__delete", 1, true);
        this.playerId = playerId;
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
        return "DeletePlayerCall{" +
                "resultCode=" + resultCode +
                ", playerId=" + playerId +
                '}';
    }
}
