package net.sf.l2j.gameserver.model.item;

/**
 * @author Johnson / 18.07.2017
 */
public enum EItemBodyPart {
    SLOT_NONE(0x0000, "none"),
    SLOT_UNDERWEAR(0x0001, "underwear"),
    SLOT_R_EAR(0x0002, "rear"),
    SLOT_L_EAR(0x0004, "lear"),
    SLOT_NECK(0x0008, "neck"),
    SLOT_R_FINGER(0x0010, "rfinger"),
    SLOT_L_FINGER(0x0020, "lfinger"),
    SLOT_HEAD(0x0040, "head"),
    SLOT_R_HAND(0x0080, "rhand"),
    SLOT_L_HAND(0x0100, "lhand"),
    SLOT_GLOVES(0x0200, "gloves"),
    SLOT_CHEST(0x0400, "chest"),
    SLOT_LEGS(0x0800, "legs"),
    SLOT_FEET(0x1000, "feet"),
    SLOT_BACK(0x2000, "back"),
    SLOT_LR_HAND(0x4000, "lrhand"),
    SLOT_FULL_ARMOR(0x8000, "fullarmor"),
    SLOT_FACE(0x010000, "face"),
    SLOT_ALLDRESS(0x020000, "alldress"),
    SLOT_HAIR(0x040000, "hair"),
    SLOT_HAIRALL(0x080000, "hairall"),

    SLOT_WOLF(-100, "wolf"),
    SLOT_HATCHLING(-101, "hatchling"),
    SLOT_STRIDER(-102, "strider"),
    SLOT_BABYPET(-103, "babypet"),

    SLOT_LR_EAR(SLOT_R_EAR.mask | SLOT_L_EAR.mask, "rear;lear"),
    SLOT_LR_FINGER(SLOT_R_FINGER.mask | SLOT_L_FINGER.mask, "rfinger;lfinger"),
    SLOT_ALLWEAPON(SLOT_LR_HAND.mask | SLOT_R_HAND.mask, "rhand;lhand");

    private final int mask;
    private final String code;

    EItemBodyPart(int mask, String code) {
        this.mask = mask;
        this.code = code;
    }

    public boolean isInBodyPart(int bodypart) {
        return (mask & bodypart) != 0;
    }

    public static boolean isInAtLeastOneBodyPart(int bodypart, EItemBodyPart... bodyParts) {
        if (bodyParts != null) {
            for (EItemBodyPart eItemBodyPart : bodyParts) {
                if (eItemBodyPart.isInBodyPart(bodypart)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static EItemBodyPart getByCode(String code) {
        for (EItemBodyPart bodyPart : values()) {
            if (bodyPart.code.equals(code)) {
                return bodyPart;
            }
        }
        return SLOT_NONE;
    }
}
