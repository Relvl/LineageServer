package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.FriendAddRequest;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public final class RequestFriendInvite extends L2GameClientPacket {
    private String name;

    @Override
    protected void readImpl() {
        name = readS();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        L2PcInstance friend = L2World.getInstance().getPlayer(name);
        if (friend == null || !friend.isOnline() || friend.isInvisible()) {
            player.sendPacket(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
            return;
        }

        if (friend == player) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST);
            return;
        }

        if (player.getContactController().isBlocked(friend)) {
            player.sendMessage("Вы игнорируете " + name + ". Сначала перестаньте его игнорировать с помощью /unblock " + name + ".");
        }
        if (friend.getContactController().isBlocked(player)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addPcName(friend));
        }

        if (player.getContactController().isFriend(friend.getObjectId())) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST).addString(name));
            return;
        }

        if (!friend.isProcessingRequest()) {
            player.onTransactionRequest(friend);
            friend.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_REQUESTED_TO_BECOME_FRIENDS).addPcName(player));
            friend.sendPacket(new FriendAddRequest(player.getName()));
        }
        else {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(name));
        }
    }
}