package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

public final class RequestAnswerFriendInvite extends L2GameClientPacket {
    private int response;

    @Override
    protected void readImpl() {
        response = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }
        L2PcInstance requestor = player.getActiveRequester();
        if (requestor == null) { return; }

        if (response == 1) {
            player.getContactController().addFriend(requestor);
        }
        else {
            requestor.sendPacket(SystemMessageId.FAILED_TO_INVITE_A_FRIEND);
        }

        player.setActiveRequester(null);
        requestor.onTransactionResponse();
    }
}