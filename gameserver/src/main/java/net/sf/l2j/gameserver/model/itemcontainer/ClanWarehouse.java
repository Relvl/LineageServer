package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EItemLocation;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;

public final class ClanWarehouse extends Warehouse {
    private final L2Clan _clan;

    public ClanWarehouse(L2Clan clan) {
        _clan = clan;
    }

    @Override
    public String getName() {
        return "ClanWarehouse";
    }

    @Override
    public EItemProcessPurpose getItemInteractionPurpose() {
        return EItemProcessPurpose.CLAN_WAREHOUSE;
    }

    @Override
    public int getOwnerId() {
        return _clan.getClanId();
    }

    @Override
    public L2PcInstance getOwner() {
        return _clan.getLeader().getPlayerInstance();
    }

    @Override
    public EItemLocation getBaseLocation() {
        return EItemLocation.CLANWH;
    }

    public static String getLocationId() {
        return "0";
    }

    public static int getLocationId(boolean dummy) {
        return 0;
    }

    public void setLocationId(L2PcInstance dummy) {
    }

    @Override
    public boolean validateCapacity(int slots) {
        return (_items.size() + slots <= Config.WAREHOUSE_SLOTS_CLAN);
    }
}