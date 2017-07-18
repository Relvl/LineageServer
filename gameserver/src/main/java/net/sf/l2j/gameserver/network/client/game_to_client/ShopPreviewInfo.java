package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.item.EPaperdollSlot;

import java.util.Map;

public class ShopPreviewInfo extends L2GameServerPacket {
    private final Map<EPaperdollSlot, Integer> itemlist;

    public ShopPreviewInfo(Map<EPaperdollSlot, Integer> itemlist) {
        this.itemlist = itemlist;
    }

    @Override
    protected final void writeImpl() {
        writeC(0xf0);
        writeD(EPaperdollSlot.values().length);
        // Slots
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_REAR)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_LEAR)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_NECK)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_RFINGER)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_LFINGER)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_HEAD)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_RHAND)); // good
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_LHAND)); // good
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_GLOVES)); // good
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_CHEST)); // good
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_LEGS)); // good
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_FEET)); // good
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_BACK)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_FACE)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_HAIR)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_HAIRALL)); // unverified
        writeD(getFromList(EPaperdollSlot.PAPERDOLL_UNDER)); // unverified
    }

    private int getFromList(EPaperdollSlot key) {
        return itemlist.containsKey(key) ? itemlist.get(key) : 0;
    }
}