package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SendTradeRequest;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

public final class TradeRequest extends L2GameClientPacket {
    private int objectId;

    @Override
    protected void readImpl() {
        objectId = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        if (!player.getAccessLevel().allowTransaction()) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
        }

        L2PcInstance target = L2World.getInstance().getPlayer(objectId);
        if (target == null || !player.getKnownList().isObjectKnown(target) || target.equals(player)) {
            player.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
            return;
        }

        if (target.isInOlympiadMode() || player.isInOlympiadMode()) {
            player.sendMessage("You or your target can't trade during Olympiad.");
            return;
        }

        // Alt game - Karma punishment
        if (!Config.KARMA_PLAYER_CAN_TRADE && (player.getKarma() > 0 || target.getKarma() > 0)) {
            player.sendMessage("Chaotic players can't trade.");
            return;
        }

        if (player.isInStoreMode() || target.isInStoreMode()) {
            player.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
            return;
        }

        if (player.isProcessingTransaction()) {
            player.sendPacket(SystemMessageId.ALREADY_TRADING);
            return;
        }

        if (target.isProcessingRequest() || target.isProcessingTransaction()) {
            SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addPcName(target);
            player.sendPacket(sm);
            return;
        }

        if (target.getContactController().isTradeRefusal()) {
            player.sendMessage("Персонаж автоматически отклоняет Ваше предложение омена.");
            return;
        }

        if (target.getContactController().isBlocked(player)) {
            SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addPcName(target);
            player.sendPacket(sm);
            return;
        }

        if (Util.calculateDistance(player, target, true) > 150) {
            player.sendPacket(SystemMessageId.TARGET_TOO_FAR);
            return;
        }

        player.onTransactionRequest(target);
        target.sendPacket(new SendTradeRequest(player.getObjectId()));
        player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.REQUEST_S1_FOR_TRADE).addPcName(target));
    }
}