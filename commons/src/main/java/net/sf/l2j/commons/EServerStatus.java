package net.sf.l2j.commons;

/**
 * @author Johnson / 11.07.2017
 */
public enum EServerStatus {
    STATUS_AUTO(0x00, "Auto"),
    STATUS_GOOD(0x01, "Good"),
    STATUS_NORMAL(0x02, "Normal"),
    STATUS_FULL(0x03, "Full"),
    STATUS_DOWN(0x04, "Down"),
    STATUS_GM_ONLY(0x05, "GM Only");

    private final int code;
    private final String text;

    EServerStatus(int code, String text) {this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static EServerStatus getByCode(int code) {
        for (EServerStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return STATUS_DOWN;
    }
}
