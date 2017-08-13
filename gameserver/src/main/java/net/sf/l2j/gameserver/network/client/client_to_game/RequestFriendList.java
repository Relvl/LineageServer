package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

import java.util.Map.Entry;

public final class RequestFriendList extends L2GameClientPacket {
    @Override
    protected void readImpl() {
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }
        player.sendPacket(SystemMessageId.FRIEND_LIST_HEADER);
        for (Entry<Integer, String> id : player.getContactController().getFriends().entrySet()) {
            L2PcInstance friend = L2World.getInstance().getPlayer(id.getKey());
            player.sendPacket(SystemMessage.getSystemMessage(friend == null || !friend.isOnline() ?
                            SystemMessageId.S1_OFFLINE :
                            SystemMessageId.S1_ONLINE
            ).addString(id.getValue()));
        }
        player.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
    }
}