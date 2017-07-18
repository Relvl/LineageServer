package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.commons.ICodeProvider;

/**
 * @author Johnson / 18.07.2017
 */
public enum EItemBodyPart implements ICodeProvider {
    SLOT_NONE(0x0000, "none", 0x00),
    SLOT_UNDERWEAR(0x0001, "underwear", 0x00),
    SLOT_R_EAR(0x0002, "rear", 0x02),
    SLOT_L_EAR(0x0004, "lear", 0x01),
    SLOT_NECK(0x0008, "neck", 0x03),
    SLOT_R_FINGER(0x0010, "rfinger", 0x04),
    SLOT_L_FINGER(0x0020, "lfinger", 0x05),
    SLOT_HEAD(0x0040, "head", 0x06),
    SLOT_R_HAND(0x0080, "rhand", 0x07),
    SLOT_L_HAND(0x0100, "lhand", 0x08),
    SLOT_GLOVES(0x0200, "gloves", 0x09),
    SLOT_CHEST(0x0400, "chest", 0x0a),
    SLOT_LEGS(0x0800, "legs", 0x0b),
    SLOT_FEET(0x1000, "feet", 0x0c),
    SLOT_BACK(0x2000, "back", 0x0d),
    SLOT_LR_HAND(0x4000, "lrhand", 0x0e),
    SLOT_FULL_ARMOR(0x8000, "fullarmor", 0x00),
    SLOT_FACE(0x010000, "face", 0x00),
    SLOT_ALLDRESS(0x020000, "alldress", 0x00),
    SLOT_HAIR(0x040000, "hair", 0x0f),
    SLOT_HAIRALL(0x080000, "hairall", 0x00),

    SLOT_WOLF(-100, "wolf", 0x00),
    SLOT_HATCHLING(-101, "hatchling", 0x00),
    SLOT_STRIDER(-102, "strider", 0x00),
    SLOT_BABYPET(-103, "babypet", 0x00),

    SLOT_LR_EAR(SLOT_R_EAR.code | SLOT_L_EAR.code, "rear;lear", 0x00),
    SLOT_LR_FINGER(SLOT_R_FINGER.code | SLOT_L_FINGER.code, "rfinger;lfinger", 0x00),
    SLOT_ALLWEAPON(SLOT_LR_HAND.code | SLOT_R_HAND.code, "rhand;lhand", 0x00);

    private final int code;
    private final String value;
    private final int equipUpdatePacketValue;

    EItemBodyPart(int code, String value, int equipUpdatePacketValue) {
        this.code = code;
        this.value = value;
        this.equipUpdatePacketValue = equipUpdatePacketValue;
    }

    @Override
    public int getCode() {
        return code;
    }

    public int getEquipUpdatePacketValue() {
        return equipUpdatePacketValue;
    }

    public boolean isInBodyPart(int bodypart) {
        return (code & bodypart) != 0;
    }

    public boolean isInBodyPart(EItemBodyPart bodypart) {
        return (code & bodypart.code) != 0;
    }

    public static boolean isInAtLeastOneBodyPart(EItemBodyPart bodypart, EItemBodyPart... bodyParts) {
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
            if (bodyPart.value.equals(code)) {
                return bodyPart;
            }
        }
        return SLOT_NONE;
    }

    public static EItemBodyPart getByMask(int mask) {
        for (EItemBodyPart bodyPart : values()) {
            if (bodyPart.code == mask) {
                return bodyPart;
            }
        }
        return SLOT_NONE;
    }
}
