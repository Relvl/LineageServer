package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;
import net.sf.l2j.gameserver.model.tradelist.TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.TradeItemUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.TradeOtherAdd;
import net.sf.l2j.gameserver.network.client.game_to_client.TradeOwnAdd;

public final class AddTradeItem extends L2GameClientPacket {
    private int _tradeId;
    private int _objectId;
    private int _count;

    public AddTradeItem() {
    }

    @Override
    protected void readImpl() {
        _tradeId = readD();
        _objectId = readD();
        _count = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        TradeList trade = player.getActiveTradeList();
        if (trade == null) {
            _log.warn("Character: " + player.getName() + " requested item:" + _objectId + " add without active tradelist:" + _tradeId);
            return;
        }

        L2PcInstance partner = trade.getPartner();
        if (partner == null || L2World.getInstance().getPlayer(partner.getObjectId()) == null || partner.getActiveTradeList() == null) {
            // Trade partner not found, cancel trade
            if (partner != null) { _log.warn(player.getName() + " requested invalid trade object: " + _objectId); }

            player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
            player.cancelActiveTrade();
            return;
        }

        if (trade.isConfirmed() || partner.getActiveTradeList().isConfirmed()) {
            player.sendPacket(SystemMessageId.CANNOT_ADJUST_ITEMS_AFTER_TRADE_CONFIRMED);
            return;
        }

        if (!player.getAccessLevel().allowTransaction()) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            player.cancelActiveTrade();
            return;
        }

        if (!player.validateItemManipulation(_objectId)) {
            player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
            return;
        }

        TradeItem item = trade.addItem(_objectId, _count);
        if (item != null) {
            player.sendPacket(new TradeOwnAdd(item));
            player.sendPacket(new TradeItemUpdate(trade, player));
            trade.getPartner().sendPacket(new TradeOtherAdd(item));
        }
    }
}