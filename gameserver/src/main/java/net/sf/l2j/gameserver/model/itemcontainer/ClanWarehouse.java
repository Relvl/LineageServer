package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;

public final class ClanWarehouse extends Warehouse {
    private final L2Clan clan;

    public ClanWarehouse(L2Clan clan) {
        this.clan = clan;
    }

    @Override
    public String getName() { return "ClanWarehouse"; }

    @Override
    public EItemProcessPurpose getItemInteractionPurpose() { return EItemProcessPurpose.CLAN_WAREHOUSE; }

    @Override
    public int getOwnerId() { return clan.getClanId(); }

    @Override
    public L2PcInstance getOwner() { return clan.getLeader().getPlayerInstance(); }

    @Override
    public EItemLocation getBaseLocation() { return EItemLocation.CLANWH; }

    @Override
    public boolean validateCapacity(int slots) { return items.size() + slots <= Config.WAREHOUSE_SLOTS_CLAN; }
}