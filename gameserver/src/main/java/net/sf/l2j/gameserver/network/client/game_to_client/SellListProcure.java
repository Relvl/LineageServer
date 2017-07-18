package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.CropProcure;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SellListProcure extends L2GameServerPacket {
    private final Map<L2ItemInstance, Integer> _sellList = new HashMap<>();
    private final List<CropProcure> _procureList;

    private final int _money;
    private final int _castle;

    public SellListProcure(L2PcInstance player, int castleId) {
        _money = player.getAdena();
        _castle = castleId;
        _procureList = CastleManager.getInstance().getCastleById(_castle).getCropProcure(0);

        for (CropProcure c : _procureList) {
            L2ItemInstance item = player.getInventory().getItemByItemId(c.getId());
            if (item != null && c.getAmount() > 0) { _sellList.put(item, c.getAmount()); }
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0xE9);
        writeD(_money); // money
        writeD(0x00); // lease ?
        writeH(_sellList.size()); // list size

        for (Entry<L2ItemInstance, Integer> itemEntry : _sellList.entrySet()) {
            L2ItemInstance item = itemEntry.getKey();
            writeH(item.getItem().getType1());
            writeD(item.getObjectId());
            writeD(item.getItemId());
            writeD(itemEntry.getValue()); // count
            writeH(item.getItem().getType2());
            writeH(0); // unknown
            writeD(0); // price, u shouldnt get any adena for crops, only raw materials
        }
    }
}