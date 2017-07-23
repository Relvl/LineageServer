package net.sf.l2j.commons.database;

/**
 * @author Johnson / 23.07.2017
 */
public enum EModify {
    ADD(0),
    UPDATE(1),
    DELETE(2);
    private final Integer mod;

    EModify(Integer mod) {this.mod = mod;}

    public Integer getMod() {
        return mod;
    }
}
