package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.commons.ICodeProvider;

/**
 * @author Johnson / 18.07.2017
 */
public enum EItemType1 implements ICodeProvider {
    WEAPON_RING_EARRING_NECKLACE(0),
    SHIELD_ARMOR(1),
    ITEM_QUESTITEM_ADENA(4);

    private final int code;

    EItemType1(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }
}
