package net.sf.l2j.gameserver.playerpart.contact;

import java.util.Objects;

/**
 * @author Johnson / 12.08.2017
 */
public enum EContactType {
    FRIEND(0),
    IGNORED(1),
    SNOOP(2),
    REMOVE(null);

    private final Integer type;

    EContactType(Integer type) {this.type = type;}

    public Integer getType() { return type; }

    public static EContactType getByType(Integer type) {
        for (EContactType contactType : values()) {
            if (Objects.equals(type, contactType.type)) {
                return contactType;
            }
        }
        return REMOVE;
    }
}
