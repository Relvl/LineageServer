package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.EnchantResult;

public final class RequestGetItemFromPet extends L2GameClientPacket {
    private int _objectId;
    private int _amount;
    @SuppressWarnings("unused")
    private int _unknown;

    @Override
    protected void readImpl() {
        _objectId = readD();
        _amount = readD();
        _unknown = readD();// = 0 for most trades
    }

    @Override
    protected void runImpl() {
        if (_amount <= 0) { return; }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null || !player.hasPet()) { return; }

        if (player.isProcessingTransaction()) {
            player.sendPacket(SystemMessageId.ALREADY_TRADING);
            return;
        }

        if (player.getActiveEnchantItem() != null) {
            player.setActiveEnchantItem(null);
            player.sendPacket(EnchantResult.CANCELLED);
            player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
        }

        L2PetInstance pet = (L2PetInstance) player.getPet();

        if (pet.transferItem(EItemProcessPurpose.PET_TRANSFER, _objectId, _amount, player.getInventory(), player, pet) == null) {
            _log.warn("Invalid item transfer request: {}(pet) --> {}", pet.getName(), player.getName());
        }
    }
}