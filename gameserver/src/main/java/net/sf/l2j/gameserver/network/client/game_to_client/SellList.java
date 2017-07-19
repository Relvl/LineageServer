package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

import java.util.ArrayList;
import java.util.List;

public class SellList extends L2GameServerPacket {
    private final L2PcInstance _activeChar;
    private final int _money;
    private final List<L2ItemInstance> _selllist = new ArrayList<>();

    public SellList(L2PcInstance player) {
        _activeChar = player;
        _money = _activeChar.getAdena();
    }

    @Override
    public final void runImpl() {
        for (L2ItemInstance item : _activeChar.getInventory().getItems()) {
            if (!item.isEquipped() && item.isSellable() && (_activeChar.getPet() == null || item.getObjectId() != _activeChar.getPet().getControlItemId())) { _selllist.add(item); }
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0x10);
        writeD(_money);
        writeD(0x00);
        writeH(_selllist.size());

        for (L2ItemInstance temp : _selllist) {
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
            writeD(item.getReferencePrice() / 2);
        }
    }
}