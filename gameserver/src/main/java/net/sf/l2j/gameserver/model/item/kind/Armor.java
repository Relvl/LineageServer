package net.sf.l2j.gameserver.model.item.kind;

import net.sf.l2j.gameserver.model.item.EItemBodyPart;
import net.sf.l2j.gameserver.model.item.EItemType1;
import net.sf.l2j.gameserver.model.item.EItemType2;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.templates.StatsSet;

public final class Armor extends Item {
    private ArmorType armorType;

    public Armor(StatsSet set) {
        super(set);
        armorType = ArmorType.valueOf(set.getString("armor_type", "none").toUpperCase());

        if (EItemBodyPart.isInAtLeastOneBodyPart(getBodyPart(),
                EItemBodyPart.SLOT_NECK,
                EItemBodyPart.SLOT_FACE,
                EItemBodyPart.SLOT_HAIR,
                EItemBodyPart.SLOT_HAIRALL,
                EItemBodyPart.SLOT_L_EAR,
                EItemBodyPart.SLOT_R_EAR,
                EItemBodyPart.SLOT_L_FINGER,
                EItemBodyPart.SLOT_R_FINGER,
                EItemBodyPart.SLOT_BACK
        )) {
            itemType1 = EItemType1.WEAPON_RING_EARRING_NECKLACE;
            itemType2 = EItemType2.TYPE2_ACCESSORY;
        }
        else {
            // retail define shield as NONE
            if (armorType == ArmorType.NONE && getBodyPart() == EItemBodyPart.SLOT_L_HAND) {
                armorType = ArmorType.SHIELD;
            }
            itemType1 = EItemType1.SHIELD_ARMOR;
            itemType2 = EItemType2.TYPE2_SHIELD_ARMOR;
        }
    }

    @Override
    public ArmorType getItemType() {
        return armorType;
    }

    @Override
    public int getItemMask() {
        return armorType.mask();
    }
}