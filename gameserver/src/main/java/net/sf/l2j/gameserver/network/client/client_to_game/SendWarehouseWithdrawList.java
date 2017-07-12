package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.ClanWarehouse;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcWarehouse;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.EnchantResult;
import net.sf.l2j.gameserver.network.client.game_to_client.InventoryUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.StatusUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

public final class SendWarehouseWithdrawList extends L2GameClientPacket {
    private static final int BATCH_LENGTH = 8; // length of one item

    private IntIntHolder _items[];

    @Override
    protected void readImpl() {
        int count = readD();
        if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != buffer.remaining()) { return; }

        _items = new IntIntHolder[count];
        for (int i = 0; i < count; i++) {
            int objId = readD();
            int cnt = readD();

            if (objId < 1 || cnt < 0) {
                _items = null;
                return;
            }
            _items[i] = new IntIntHolder(objId, cnt);
        }
    }

    @Override
    protected void runImpl() {
        if (_items == null) { return; }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        if (player.isProcessingTransaction()) {
            player.sendPacket(SystemMessageId.ALREADY_TRADING);
            return;
        }

        if (player.getActiveEnchantItem() != null) {
            player.setActiveEnchantItem(null);
            player.sendPacket(EnchantResult.CANCELLED);
            player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
        }

        ItemContainer warehouse = player.getActiveWarehouse();
        if (warehouse == null) { return; }

        L2Npc manager = player.getCurrentFolkNPC();
        if (manager == null || !manager.isWarehouse() || !manager.canInteract(player)) { return; }

        if (!(warehouse instanceof PcWarehouse) && !player.getAccessLevel().allowTransaction()) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
        }

        // Alt game - Karma punishment
        if (!Config.KARMA_PLAYER_CAN_USE_WH && player.getKarma() > 0) { return; }

        if (Config.ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH) {
            if (warehouse instanceof ClanWarehouse && ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)) {
                return;
            }
        }
        else {
            if (warehouse instanceof ClanWarehouse && !player.isClanLeader()) {
                // this msg is for depositing but maybe good to send some msg?
                player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CAN_RETRIEVE_ITEMS_FROM_CLAN_WAREHOUSE);
                return;
            }
        }

        int weight = 0;
        int slots = 0;

        for (IntIntHolder i : _items) {
            // Calculate needed slots
            ItemInstance item = warehouse.getItemByObjectId(i.getId());
            if (item == null || item.getCount() < i.getValue()) {
                Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " tried to withdraw non-existent item from warehouse.", Config.DEFAULT_PUNISH);
                return;
            }

            weight += i.getValue() * item.getItem().getWeight();

            if (!item.isStackable()) { slots += i.getValue(); }
            else if (player.getInventory().getItemByItemId(item.getItemId()) == null) { slots++; }
        }

        // Item Max Limit Check
        if (!player.getInventory().validateCapacity(slots)) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
            return;
        }

        // Weight limit Check
        if (!player.getInventory().validateWeight(weight)) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
            return;
        }

        // Proceed to the transfer
        InventoryUpdate playerIU = new InventoryUpdate();
        for (IntIntHolder i : _items) {
            ItemInstance oldItem = warehouse.getItemByObjectId(i.getId());
            if (oldItem == null || oldItem.getCount() < i.getValue()) {
                _log.warn("Error withdrawing a warehouse object for {} (olditem == null)", player.getName());
                return;
            }

            ItemInstance newItem = warehouse.transferItem(warehouse.getName(), i.getId(), i.getValue(), player.getInventory(), player, manager);
            if (newItem == null) {
                _log.warn("Error withdrawing a warehouse object for {} (newitem == null)", player.getName());
                return;
            }

            if (newItem.getCount() > i.getValue()) { playerIU.addModifiedItem(newItem); }
            else { playerIU.addNewItem(newItem); }
        }

        // Send updated item list to the player
        player.sendPacket(playerIU);

        // Update current load status on player
        StatusUpdate su = new StatusUpdate(player);
        su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
        player.sendPacket(su);
    }
}