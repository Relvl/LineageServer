package net.sf.l2j.gameserver.model.item.kind;

import net.sf.l2j.gameserver.model.item.EItemBodyPart;
import net.sf.l2j.gameserver.model.item.EItemType1;
import net.sf.l2j.gameserver.model.item.EItemType2;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * This class is dedicated to the management of armors.
 */
public final class Armor extends Item {
    private ArmorType _type;

    /**
     * Constructor for Armor.<BR>
     * <BR>
     * <U><I>Variables filled :</I></U><BR>
     * <LI>_avoidModifier</LI> <LI>_pDef & _mDef</LI> <LI>_mpBonus & _hpBonus</LI>
     *
     * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
     * @see Item constructor
     */
    public Armor(StatsSet set) {
        super(set);
        _type = ArmorType.valueOf(set.getString("armor_type", "none").toUpperCase());

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
            _type2 = EItemType2.TYPE2_ACCESSORY;
        }
        else {
            // retail define shield as NONE
            if (_type == ArmorType.NONE && getBodyPart() == EItemBodyPart.SLOT_L_HAND) {
                _type = ArmorType.SHIELD;
            }

            itemType1 = EItemType1.SHIELD_ARMOR;
            _type2 = EItemType2.TYPE2_SHIELD_ARMOR;
        }
    }

    /**
     * Returns the type of the armor.
     *
     * @return ArmorType
     */
    @Override
    public ArmorType getItemType() {
        return _type;
    }

    /**
     * Returns the ID of the item after applying the mask.
     *
     * @return int : ID of the item
     */
    @Override
    public int getItemMask() {
        return _type.mask();
    }
}