package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.tradelist.TradeItem;

public class TradeOtherAdd extends L2GameServerPacket {
    private final TradeItem _item;

    public TradeOtherAdd(TradeItem item) {
        _item = item;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x21);

        writeH(1); // item count

        writeH(_item.getItem().getType1()); // item type1
        writeD(_item.getObjectId());
        writeD(_item.getItem().getItemId());
        writeD(_item.getCount());
        writeH(_item.getItem().getType2()); // item type2
        writeH(0x00); // ?
        writeD(_item.getItem().getBodyPart());
        writeH(_item.getEnchant()); // enchant level
        writeH(0x00); // ?
        writeH(0x00);
    }
}
