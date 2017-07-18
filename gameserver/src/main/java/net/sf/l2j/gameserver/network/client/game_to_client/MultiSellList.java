package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.multisell.Entry;
import net.sf.l2j.gameserver.model.multisell.Ingredient;
import net.sf.l2j.gameserver.model.multisell.ListContainer;

public class MultiSellList extends L2GameServerPacket {
    protected int _listId, _page, _finished;
    protected ListContainer _list;

    public MultiSellList(ListContainer list, int page, int finished) {
        _list = list;
        _listId = list.getListId();
        _page = page;
        _finished = finished;
    }

    @Override
    protected void writeImpl() {
        writeC(0xd0);
        writeD(_listId); // list id
        writeD(_page); // page
        writeD(_finished); // finished
        writeD(0x28); // size of pages
        writeD(_list == null ? 0 : _list.getEntries().size()); // list lenght

        if (_list != null) {
            for (Entry ent : _list.getEntries()) {
                writeD(ent.getEntryId());
                writeD(0x00); // C6
                writeD(0x00); // C6
                writeC(1);
                writeH(ent.getProducts().size());
                writeH(ent.getIngredients().size());

                for (Ingredient i : ent.getProducts()) {
                    Item item = ItemTable.getInstance().getTemplate(i.getItemId());
                    writeH(i.getItemId());
                    writeD(item.getBodyPart());
                    writeH(item.getType2());
                    writeD(i.getItemCount());
                    writeH(i.getEnchantmentLevel());
                    writeD(0x00); // TODO: i.getAugmentId()
                    writeD(0x00); // TODO: i.getManaLeft()
                }

                for (Ingredient i : ent.getIngredients()) {
                    int itemId = i.getItemId();
                    Item item = ItemTable.getInstance().getTemplate(itemId);

                    writeH(itemId);
                    writeH((itemId == 65336) ? 65535 : item.getType2().getCode());
                    writeD(i.getItemCount());
                    writeH(i.getEnchantmentLevel());
                    writeD(0x00); // TODO: i.getAugmentId()
                    writeD(0x00); // TODO: i.getManaLeft()
                }
            }
        }
    }
}