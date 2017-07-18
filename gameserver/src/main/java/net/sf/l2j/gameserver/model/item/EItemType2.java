package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.commons.ICodeProvider;

/**
 * @author Johnson / 18.07.2017
 */
public enum EItemType2 implements ICodeProvider {
    TYPE2_WEAPON(0),
    TYPE2_SHIELD_ARMOR(1),
    TYPE2_ACCESSORY(2),
    TYPE2_QUEST(3),
    TYPE2_MONEY(4),
    TYPE2_OTHER(5);

    private final int code;

    EItemType2(int code) {this.code = code;}

    @Override
    public int getCode() {
        return code;
    }
}
