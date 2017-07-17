package net.sf.l2j.gameserver.model.zone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Johnson / 17.07.2017
 */
public enum EZoneShape {
    CUBOID("Cuboid"),
    CYLINDER("Cylinder"),
    NPOLY("NPoly"),
    UNKNOWN("");

    private final String code;

    EZoneShape(String code) {
        this.code = code;
    }

    @JsonCreator
    public static EZoneShape getByCode(String code) {
        for (EZoneShape zoneShape : values()) {
            if (zoneShape.code.equals(code)) {
                return zoneShape;
            }
        }
        return UNKNOWN;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
