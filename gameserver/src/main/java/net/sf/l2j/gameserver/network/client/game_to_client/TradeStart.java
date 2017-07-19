package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

/**
 * d h (h dddhh dhhh)
 */
public class TradeStart extends L2GameServerPacket {
    private final L2PcInstance _activeChar;
    private final L2ItemInstance[] _itemList;

    public TradeStart(L2PcInstance player) {
        _activeChar = player;
        _itemList = player.getInventory().getAvailableItems(true, false);
    }

    @Override
    protected final void writeImpl() {
        if (_activeChar.getActiveTradeList() == null || _activeChar.getActiveTradeList().getPartner() == null) { return; }

        writeC(0x1E);
        writeD(_activeChar.getActiveTradeList().getPartner().getObjectId());
        writeH(_itemList.length);

        for (L2ItemInstance temp : _itemList) {
            if (temp == null || temp.getItem() == null) { continue; }

            Item item = temp.getItem();
            writeH(item.getType1());
            writeD(temp.getObjectId());
            writeD(temp.getItemId());
            writeD(temp.getCount());
            writeH(item.getType2());
            writeH(temp.getCustomType1());
            writeD(item.getBodyPart());
            writeH(temp.getEnchantLevel());
            writeH(temp.getCustomType2());
            writeH(0x00);
        }
    }
}