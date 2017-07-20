package net.sf.l2j.gameserver.model.item.kind;

import net.sf.l2j.gameserver.model.item.EItemType1;
import net.sf.l2j.gameserver.model.item.EItemType2;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.templates.StatsSet;

public final class EtcItem extends Item {
    private EtcItemType etcItemType;
    private final String handler;
    private final int reuseDelay;
    private final int sharedReuseGroup;

    public EtcItem(StatsSet set) {
        super(set);
        itemType1 = EItemType1.ITEM_QUESTITEM_ADENA;
        itemType2 = EItemType2.TYPE2_OTHER;

        etcItemType = EtcItemType.valueOf(set.getString("etcitem_type", "none").toUpperCase());

        switch (getDefaultAction()) {
            case soulshot:
            case summon_soulshot:
            case summon_spiritshot:
            case spiritshot:
                etcItemType = EtcItemType.SHOT;
                break;
        }

        if (isQuestItem()) {
            itemType2 = EItemType2.TYPE2_QUEST;
        }
        else if (getItemId() == PcInventory.ADENA_ID || getItemId() == PcInventory.ANCIENT_ADENA_ID) {
            itemType2 = EItemType2.TYPE2_MONEY;
        }

        handler = set.getString("handler", null);

        reuseDelay = set.getInteger("reuse_delay", 0);
        sharedReuseGroup = set.getInteger("shared_reuse_group", -1);
    }

    @Override
    public EtcItemType getItemType() {
        return etcItemType;
    }

    @Override
    public boolean isConsumable() {
        return (etcItemType == EtcItemType.SHOT) || (etcItemType == EtcItemType.POTION);
    }

    @Override
    public int getItemMask() {
        return etcItemType.mask();
    }

    public String getHandlerName() {
        return handler;
    }

    public int getSharedReuseGroup() {
        return sharedReuseGroup;
    }

    public int getReuseDelay() {
        return reuseDelay;
    }
}