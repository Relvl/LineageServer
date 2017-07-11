package net.sf.l2j.commons;

/**
 * @author Johnson / 11.07.2017
 */
public enum EGameServerLoginFailReason {
    REASON_IP_BANNED(0x01, "Reason: ip banned"),
    REASON_IP_RESERVED(0x02, "Reason: ip reserved"),
    REASON_WRONG_HEXID(0x03, "Reason: wrong hexid"),
    REASON_ID_RESERVED(0x04, "Reason: id reserved"),
    REASON_NO_FREE_ID(0x05, "Reason: no free ID"),
    REASON_NOT_AUTHED(0x06, "Reason: not authed"),
    REASON_ALREADY_LOGGED_IN(0x07, "Reason: already logged in"),
    REASON_UNKNOWN(0x00, "Reason: unknown");

    private final int code;
    private final String text;

    EGameServerLoginFailReason(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static EGameServerLoginFailReason getByCode(int code) {
        for (EGameServerLoginFailReason reason : values()) {
            if (reason.code == code) {
                return reason;
            }
        }
        return REASON_UNKNOWN;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }
}
