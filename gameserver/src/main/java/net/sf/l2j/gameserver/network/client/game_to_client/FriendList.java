package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class FriendList extends L2GameServerPacket {
    private final List<FriendInfo> friendInfos;

    private static final class FriendInfo {
        private final int objectId;
        private final String name;
        private final boolean online;

        private FriendInfo(int objectId, String name, boolean online) {
            this.objectId = objectId;
            this.name = name;
            this.online = online;
        }
    }

    public FriendList(L2PcInstance player) {
        friendInfos = new ArrayList<>();
        for (Entry<Integer, String> friendEntry : player.getContactController().getFriends().entrySet()) {
            L2PcInstance friend = L2World.getInstance().getPlayer(friendEntry.getKey());
            friendInfos.add(new FriendInfo(friendEntry.getKey(), friendEntry.getValue(), friend != null && friend.isOnline()));
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0xFA);
        writeD(friendInfos.size());
        for (FriendInfo info : friendInfos) {
            writeD(info.objectId);
            writeS(info.name);
            writeD(info.online ? 0x01 : 0x00);
            writeD(info.online ? info.objectId : 0x00);
        }
    }
}