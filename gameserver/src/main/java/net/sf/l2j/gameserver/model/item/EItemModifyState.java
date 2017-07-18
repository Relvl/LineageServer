package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.commons.ICodeProvider;

/**
 * @author Johnson / 18.07.2017
 */
public enum EItemModifyState implements ICodeProvider {
    UNCHANGED(0),
    ADDED(1),
    MODIFIED(2),
    REMOVED(3);

    private final int code;

    EItemModifyState(int code) {this.code = code;}

    @Override
    public int getCode() {
        return code;
    }
}
