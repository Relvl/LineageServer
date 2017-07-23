package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class ExStorageMaxCount extends L2GameServerPacket {
    private final int _inventoryLimit;
    private final int _warehouseLimit;
    private final int _freightLimit;
    private final int _privateSellLimit;
    private final int _privateBuyLimit;
    private final int _dwarfRecipeLimit;
    private final int _commonRecipeLimit;

    public ExStorageMaxCount(L2PcInstance player) {
        _inventoryLimit = player.getInventoryLimit();
        _warehouseLimit = player.getWareHouseLimit();
        _freightLimit = player.getFreightLimit();
        _privateSellLimit = player.getPrivateSellStoreLimit();
        _privateBuyLimit = player.getPrivateBuyStoreLimit();
        _dwarfRecipeLimit = player.getRecipeController().getDwarfRecipeLimit();
        _commonRecipeLimit = player.getRecipeController().getCommonRecipeLimit();
    }

    @Override
    protected void writeImpl() {
        writeC(0xfe);
        writeH(0x2e);
        writeD(_inventoryLimit);
        writeD(_warehouseLimit);
        writeD(_freightLimit);
        writeD(_privateSellLimit);
        writeD(_privateBuyLimit);
        writeD(_dwarfRecipeLimit);
        writeD(_commonRecipeLimit);
    }
}