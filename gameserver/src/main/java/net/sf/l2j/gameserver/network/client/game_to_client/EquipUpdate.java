package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.item.instance.L2ItemInstance;

/** FIXME Выяснить, для чего этот пакет, и заюзать его. */
public class EquipUpdate extends L2GameServerPacket {
    private final L2ItemInstance _item;
    private final int _change;

    public EquipUpdate(L2ItemInstance item, int change) {
        _item = item;
        _change = change;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x4b);
        writeD(_change);
        writeD(_item.getObjectId());
        writeD(_item.getItem().getBodyPart().getEquipUpdatePacketValue());
    }
}