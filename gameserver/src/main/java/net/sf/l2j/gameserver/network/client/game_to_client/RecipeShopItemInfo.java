package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class RecipeShopItemInfo extends L2GameServerPacket {
    private final L2PcInstance _player;
    private final int _recipeId;

    public RecipeShopItemInfo(L2PcInstance player, int recipeId) {
        _player = player;
        _recipeId = recipeId;
    }

    @Override
    protected final void writeImpl() {
        writeC(0xda);
        writeD(_player.getObjectId());
        writeD(_recipeId);
        writeD((int) _player.getCurrentMp());
        writeD(_player.getMaxMp());
        writeD(0xffffffff);
    }
}