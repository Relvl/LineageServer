package net.sf.l2j.gameserver.model.item;

/**
 * @author Johnson / 18.07.2017
 */
public enum EPaperdollSlot {
    PAPERDOLL_UNDER,
    PAPERDOLL_LEAR,
    PAPERDOLL_REAR,
    PAPERDOLL_NECK,
    PAPERDOLL_LFINGER,
    PAPERDOLL_RFINGER,
    PAPERDOLL_HEAD,
    PAPERDOLL_RHAND,
    PAPERDOLL_LHAND,
    PAPERDOLL_GLOVES,
    PAPERDOLL_CHEST,
    PAPERDOLL_LEGS,
    PAPERDOLL_FEET,
    PAPERDOLL_BACK,
    PAPERDOLL_FACE,
    PAPERDOLL_HAIR,
    PAPERDOLL_HAIRALL;

    public static EPaperdollSlot getByIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }
}
