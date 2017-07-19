package net.sf.l2j.gameserver.model.item.type;

public enum EtcItemType implements EMaskedItemType {
    NONE,
    ARROW,
    POTION,
    SCRL_ENCHANT_WP,
    SCRL_ENCHANT_AM,
    SCROLL,
    RECIPE,
    MATERIAL,
    PET_COLLAR,
    CASTLE_GUARD,
    LOTTO,
    RACE_TICKET,
    DYE,
    SEED,
    CROP,
    MATURECROP,
    HARVEST,
    SEED2,
    TICKET_OF_LORD,
    LURE,
    BLESS_SCRL_ENCHANT_WP,
    BLESS_SCRL_ENCHANT_AM,
    COUPON,
    ELIXIR,

    // L2J CUSTOM, BACKWARD COMPATIBILITY
    SHOT,
    HERB,
    QUEST;

    @Override
    public int mask() {
        return 0;
    }
}