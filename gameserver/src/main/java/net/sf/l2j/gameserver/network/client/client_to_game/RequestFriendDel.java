package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public final class RequestFriendDel extends L2GameClientPacket {
    private String name;

    @Override
    protected void readImpl() {
        name = readS();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }
        int targetContactId = CharNameTable.getInstance().getIdByName(name);
        if (targetContactId == -1 || !player.getContactController().isFriend(targetContactId)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_NOT_ON_YOUR_FRIENDS_LIST).addString(name));
            return;
        }
        player.getContactController().removeFriend(targetContactId);
    }
}