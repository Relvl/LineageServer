package net.sf.l2j.gameserver.model.item.type;

public enum ArmorType implements EMaskedItemType {
    NONE,
    LIGHT,
    HEAVY,
    MAGIC,
    PET,
    SHIELD;

    final int mask;

    ArmorType() {
        mask = 1 << (ordinal() + EWeaponType.values().length);
    }

    @Override
    public int mask() {
        return mask;
    }
}