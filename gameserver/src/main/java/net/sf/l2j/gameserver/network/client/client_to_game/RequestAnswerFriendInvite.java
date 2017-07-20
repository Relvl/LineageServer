package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.FriendList;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public final class RequestAnswerFriendInvite extends L2GameClientPacket {
    private int _response;

    @Override
    protected void readImpl() {
        _response = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        L2PcInstance requestor = player.getActiveRequester();
        if (requestor == null) { return; }

        if (_response == 1) {
            try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (char_id, friend_id) VALUES (?,?), (?,?)");
                statement.setInt(1, requestor.getObjectId());
                statement.setInt(2, player.getObjectId());
                statement.setInt(3, player.getObjectId());
                statement.setInt(4, requestor.getObjectId());
                statement.execute();
                statement.close();

                requestor.sendPacket(SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND);

                // Player added to your friendlist
                requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED_TO_FRIENDS).addPcName(player));
                requestor.getFriendList().add(player.getObjectId());

                // has joined as friend.
                player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_JOINED_AS_FRIEND).addPcName(requestor));
                player.getFriendList().add(requestor.getObjectId());

                // update friendLists *heavy method*
                requestor.sendPacket(new FriendList(requestor));
                player.sendPacket(new FriendList(player));
            } catch (Exception e) {
                _log.error("could not add friend objectid: " + e);
            }
        }
        else { requestor.sendPacket(SystemMessageId.FAILED_TO_INVITE_A_FRIEND); }

        player.setActiveRequester(null);
        requestor.onTransactionResponse();
    }
}