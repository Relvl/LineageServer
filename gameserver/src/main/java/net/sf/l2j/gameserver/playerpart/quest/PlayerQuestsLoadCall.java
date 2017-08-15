package net.sf.l2j.gameserver.playerpart.quest;

import net.sf.l2j.commons.database.IndexedCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 14.08.2017
 */
public class PlayerQuestsLoadCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerQuestsLoadCall.class);

    protected PlayerQuestsLoadCall(String procedureName, int argumentsCount, boolean isFunction) {
        super(procedureName, argumentsCount, isFunction);
    }

    @Override
    public Logger getLogger() {
        return null;
    }
}
