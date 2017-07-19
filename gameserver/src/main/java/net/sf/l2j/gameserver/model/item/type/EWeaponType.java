package net.sf.l2j.gameserver.model.item.type;

public enum EWeaponType implements EMaskedItemType {
    NONE(40),
    SWORD(40),
    BLUNT(40),
    DAGGER(40),
    BOW(500),
    POLE(66),
    ETC(40),
    FIST(40),
    DUAL(40),
    DUALFIST(40),
    BIGSWORD(40),
    FISHINGROD(40),
    BIGBLUNT(40),
    PET(40);

    private final int mask;
    private final int attackRange;

    EWeaponType(int range) {
        mask = 1 << ordinal();
        attackRange = range;
    }

    @Override
    public int mask() { return mask; }

    public int getAttackRange() { return attackRange; }
}