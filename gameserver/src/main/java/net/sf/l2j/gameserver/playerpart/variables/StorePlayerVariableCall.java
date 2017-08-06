package net.sf.l2j.gameserver.playerpart.variables;

import net.sf.l2j.commons.database.IndexedCall;
import net.sf.l2j.commons.database.annotation.OrmParamIn;
import net.sf.l2j.commons.database.annotation.OrmParamOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johnson / 20.07.2017
 */
public class StorePlayerVariableCall extends IndexedCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorePlayerVariableCall.class);

    @OrmParamOut(1)
    private Integer resultCode;

    @OrmParamIn(2)
    private final Integer pi_player;
    @OrmParamIn(3)
    private final String pi_var_name;
    @OrmParamIn(4)
    private final String pi_str_value;
    @OrmParamIn(5)
    private final Integer pi_int_value;
    @OrmParamIn(6)
    private final Boolean pi_bool_value;
    @OrmParamIn(7)
    private final Long pi_long_value;

    public StorePlayerVariableCall(Integer playerId, String name, String strVal, Integer intVal, Boolean boolVal, Long longValue) {
        super("player_variable_store", 6, true);
        this.pi_player = playerId;
        this.pi_var_name = name;
        this.pi_str_value = strVal;
        this.pi_int_value = intVal;
        this.pi_bool_value = boolVal;
        this.pi_long_value = longValue;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Integer getResultCode() {
        return resultCode;
    }
}
