package net.sf.l2j.gameserver.playerpart.variables;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johnson / 20.07.2017
 */
public class GetPlayerVariablesCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlayerVariablesCall.class);

    @OrmParamIn(1)
    private final Integer playerId;
    @OrmParamOut(value = 2, cursorClass = PlayerVariable.class)
    private final List<PlayerVariable> playerVariables = new ArrayList<>();
    @OrmParamOut(3)
    private Integer resultCode;

    protected GetPlayerVariablesCall(Integer playerId) {
        super("public.player_variable_load", 3, false);
        this.playerId = playerId;
    }

    public List<PlayerVariable> getPlayerVariables() {
        return playerVariables;
    }

    @Override
    public Integer getResultCode() {
        return resultCode;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
