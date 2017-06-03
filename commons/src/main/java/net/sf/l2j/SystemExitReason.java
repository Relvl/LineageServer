package net.sf.l2j;

/**
 * @author Johnson / 02.06.2017
 */
public enum SystemExitReason {
    SHUTDOWN(0),
    ERROR(1),
    RESTART(2);

    private final int code;

    SystemExitReason(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void perform() {
        System.exit(code);
        // Runtime.getRuntime().exit(code);
    }
}
