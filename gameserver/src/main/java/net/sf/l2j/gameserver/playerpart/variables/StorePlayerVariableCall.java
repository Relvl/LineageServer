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
    private final Integer playerId;
    @OrmParamIn(3)
    private String variableName;
    @OrmParamIn(4)
    private String strValue;
    @OrmParamIn(5)
    private Integer intValue;
    @OrmParamIn(6)
    private Boolean boolValue;
    @OrmParamIn(7)
    private Long longValue;

    public StorePlayerVariableCall(Integer playerId, String name, String strVal, Integer intVal, Boolean boolVal, Long longValue) {
        super("player_variable_store", 6, true);
        this.playerId = playerId;
        this.variableName = name;
        this.strValue = strVal;
        this.intValue = intVal;
        this.boolValue = boolVal;
        this.longValue = longValue;
    }

    @Override
    public Logger getLogger() { return LOGGER; }

    @Override
    public Integer getResultCode() { return resultCode; }

    public void setVariableName(String variableName) { this.variableName = variableName; }

    public void setStrValue(String strValue) { this.strValue = strValue; }

    public void setIntValue(Integer intValue) { this.intValue = intValue; }

    public void setBoolValue(Boolean boolValue) { this.boolValue = boolValue; }

    public void setLongValue(Long longValue) { this.longValue = longValue; }

    public void reset() {
        this.variableName = null;
        this.strValue = null;
        this.intValue = null;
        this.boolValue = null;
        this.longValue = null;
    }
}
