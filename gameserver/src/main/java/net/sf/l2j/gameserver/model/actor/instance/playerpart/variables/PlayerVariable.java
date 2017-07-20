package net.sf.l2j.gameserver.model.actor.instance.playerpart.variables;

import net.sf.l2j.commons.DefaultConstructor;
import net.sf.l2j.commons.database.annotation.OrmParamCursor;

/**
 * @author Johnson / 20.07.2017
 */
public final class PlayerVariable {
    @OrmParamCursor("VAR_NAME")
    private String name;
    @OrmParamCursor("INT_VALUE")
    private Integer intValue;
    @OrmParamCursor("BOOL_VALUE")
    private Boolean boolValue;
    @OrmParamCursor("STR_VALUE")
    private String stringValue;

    @DefaultConstructor
    public PlayerVariable() {
    }

    public PlayerVariable(String name, Integer intValue, Boolean boolValue, String stringValue) {
        this.name = name;
        this.intValue = intValue;
        this.boolValue = boolValue;
        this.stringValue = stringValue;
    }

    public String getName() {
        return name;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public Boolean getBoolValue() {
        return boolValue;
    }

    public String getStringValue() {
        return stringValue;
    }
}
