package net.sf.l2j.gameserver.playerpart;

/**
 * @author Johnson / 19.07.2017
 */
public enum PunishLevel {
    NONE(0, ""),
    CHAT(1, "chat banned"),
    JAIL(2, "jailed"),
    CHAR(3, "banned"),
    ACC(4, "banned");

    private final int punValue;
    private final String punString;

    PunishLevel(int value, String string) {
        punValue = value;
        punString = string;
    }

    public int value() {
        return punValue;
    }

    public String string() {
        return punString;
    }
}
