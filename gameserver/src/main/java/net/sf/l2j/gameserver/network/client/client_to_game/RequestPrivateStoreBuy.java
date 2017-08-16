/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.ItemRequest;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.tradelist.TradeList;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.playerpart.PrivateStoreType;
import net.sf.l2j.gameserver.util.Util;

import java.util.HashSet;
import java.util.Set;

public final class RequestPrivateStoreBuy extends L2GameClientPacket {
    private static final int BATCH_LENGTH = 12; // length of one item

    private int _storePlayerId;
    private Set<ItemRequest> _items = null;

    @Override
    protected void readImpl() {
        _storePlayerId = readD();
        int count = readD();
        if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != buffer.remaining()) { return; }

        _items = new HashSet<>();

        for (int i = 0; i < count; i++) {
            int objectId = readD();
            long cnt = readD();
            int price = readD();

            if (objectId < 1 || cnt < 1 || price < 0) {
                _items = null;
                return;
            }

            _items.add(new ItemRequest(objectId, (int) cnt, price));
        }
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        if (_items == null) { return; }

        L2PcInstance storePlayer = L2World.getInstance().getPlayer(_storePlayerId);
        if (storePlayer == null) { return; }

        if (player.isCursedWeaponEquipped()) { return; }

        if (!player.isInsideRadius(storePlayer, 150, true, false)) { return; }

        if (!(storePlayer.getPrivateStoreType() == PrivateStoreType.SELL || storePlayer.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL)) {
            return;
        }

        TradeList storeList = storePlayer.getSellList();
        if (storeList == null) { return; }

        if (!player.getAccessLevel().allowTransaction()) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
        }

        if (storePlayer.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL) {
            if (storeList.getItems().size() > _items.size()) {
                Util.handleIllegalPlayerAction(getClient().getActiveChar(), "[RequestPrivateStoreBuy] " + player.getName() + " tried to buy less items than sold in package.", Config.DEFAULT_PUNISH);
                return;
            }
        }

        int result = storeList.privateStoreBuy(player, _items);
        if (result > 0) {
            if (result > 1) {
                _log.warn("PrivateStore buy has failed due to invalid list or request. Player: {}, Private store of: {}", player.getName(), storePlayer.getName());
            }
            return;
        }

        if (storeList.getItems().isEmpty()) {
            storePlayer.setPrivateStoreType(PrivateStoreType.NONE);
            storePlayer.broadcastUserInfo();
        }
    }
}