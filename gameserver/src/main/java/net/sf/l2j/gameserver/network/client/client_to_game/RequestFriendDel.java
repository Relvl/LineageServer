package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.FriendList;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public final class RequestFriendDel extends L2GameClientPacket {
    private String _name;

    @Override
    protected void readImpl() {
        _name = readS();
    }

    @Override
    protected void runImpl() {
        final L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        int id = CharNameTable.getInstance().getIdByName(_name);

        if (id == -1 || !activeChar.getFriendList().contains(id)) {
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_NOT_ON_YOUR_FRIENDS_LIST).addString(_name));
            return;
        }

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE (char_id = ? AND friend_id = ?) OR (char_id = ? AND friend_id = ?)");
            statement.setInt(1, activeChar.getObjectId());
            statement.setInt(2, id);
            statement.setInt(3, id);
            statement.setInt(4, activeChar.getObjectId());
            statement.execute();
            statement.close();

            // Player deleted from your friendlist
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST).addString(_name));

            activeChar.getFriendList().remove(Integer.valueOf(id));
            activeChar.sendPacket(new FriendList(activeChar)); // update friendList *heavy method*

            L2PcInstance player = L2World.getInstance().getPlayer(_name);
            if (player != null) {
                player.getFriendList().remove(Integer.valueOf(activeChar.getObjectId()));
                player.sendPacket(new FriendList(player)); // update friendList *heavy method*
            }
        } catch (Exception e) {
            _log.error("could not delete friend objectid: ", e);
        }
    }
}