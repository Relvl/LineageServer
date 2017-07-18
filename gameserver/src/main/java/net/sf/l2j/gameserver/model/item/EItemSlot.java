package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

/**
 * @author Johnson / 18.07.2017
 */
@SuppressWarnings("MagicNumber")
public enum EItemSlot {
    SLOT_NONE(0x0000, null, "none"),
    SLOT_UNDERWEAR(0x0001, Inventory.PAPERDOLL_UNDER, "underwear"),
    SLOT_R_EAR(0x0002, Inventory.PAPERDOLL_REAR, "rear"),
    SLOT_L_EAR(0x0004, Inventory.PAPERDOLL_LEAR, "lear"),
    SLOT_LR_EAR(0x00006, null, "rear;lear"),
    SLOT_NECK(0x0008, Inventory.PAPERDOLL_NECK, "neck"),
    SLOT_R_FINGER(0x0010, Inventory.PAPERDOLL_RFINGER, "rfinger"),
    SLOT_L_FINGER(0x0020, Inventory.PAPERDOLL_LFINGER, "lfinger"),
    SLOT_LR_FINGER(0x0030, null, "rfinger;lfinger"),
    SLOT_HEAD(0x0040, Inventory.PAPERDOLL_HEAD, "head"),
    SLOT_R_HAND(0x0080, Inventory.PAPERDOLL_RHAND, "rhand"),
    SLOT_L_HAND(0x0100, Inventory.PAPERDOLL_LHAND, "lhand"),
    SLOT_GLOVES(0x0200, Inventory.PAPERDOLL_GLOVES, "gloves"),
    SLOT_CHEST(0x0400, Inventory.PAPERDOLL_CHEST, "chest"),
    SLOT_LEGS(0x0800, Inventory.PAPERDOLL_LEGS, "legs"),
    SLOT_FEET(0x1000, Inventory.PAPERDOLL_FEET, "feet"),
    SLOT_BACK(0x2000, Inventory.PAPERDOLL_BACK, "back"),
    SLOT_LR_HAND(0x4000, Inventory.PAPERDOLL_RHAND, "lrhand"),
    SLOT_FULL_ARMOR(0x8000, Inventory.PAPERDOLL_CHEST, "fullarmor"),
    SLOT_FACE(0x010000, Inventory.PAPERDOLL_FACE, "face"),
    SLOT_ALLDRESS(0x020000, Inventory.PAPERDOLL_CHEST, "alldress"),
    SLOT_HAIR(0x040000, Inventory.PAPERDOLL_HAIR, "hair"),
    SLOT_HAIRALL(0x080000, Inventory.PAPERDOLL_FACE, "hairall"),

    SLOT_WOLF(-100, null, "wolf"),
    SLOT_HATCHLING(-101, null, "hatchling"),
    SLOT_STRIDER(-102, null, "strider"),
    SLOT_BABYPET(-103, null, "babypet"),

    SLOT_ALLWEAPON(SLOT_LR_HAND.bodyPart | SLOT_R_HAND.bodyPart, null, "allweapon"),
    SLOT_ANY_CHEST_LEGS(SLOT_CHEST.bodyPart | SLOT_LEGS.bodyPart, null, "chest,legs");

    private final int bodyPart;
    private final Integer paperDollSlot;
    private final String code;

    EItemSlot(int bodyPart, Integer paperDollSlot, String code) {
        this.bodyPart = bodyPart;
        this.paperDollSlot = paperDollSlot;
        this.code = code;
    }

    public int getBodyPart() { return bodyPart; }

    public String getCode() { return code; }

    public Integer getPaperDollSlot() { return paperDollSlot; }

    public static EItemSlot getByBodyPart(int bodyPart) {
        for (EItemSlot itemSlot : values()) {
            if (itemSlot.bodyPart == bodyPart) {
                return itemSlot;
            }
        }
        return SLOT_NONE;
    }

    public static EItemSlot getByCode(String code) {
        for (EItemSlot itemSlot : values()) {
            if (itemSlot.code.equals(code)) {
                return itemSlot;
            }
        }
        return SLOT_NONE;
    }

}
