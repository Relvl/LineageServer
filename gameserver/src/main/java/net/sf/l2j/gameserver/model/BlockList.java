package net.sf.l2j.gameserver.model;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockList {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockList.class);
    private static Map<Integer, List<Integer>> _offlineList = new HashMap<>();

    private final L2PcInstance _owner;
    private List<Integer> _blockList;

    public BlockList(L2PcInstance owner) {
        _owner = owner;
        _blockList = _offlineList.get(owner.getObjectId());
        if (_blockList == null) { _blockList = loadList(_owner.getObjectId()); }
    }

    private static List<Integer> loadList(int ObjId) {
        List<Integer> list = new ArrayList<>();

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id = ? AND relation = 1");
            statement.setInt(1, ObjId);
            ResultSet rset = statement.executeQuery();

            int friendId;
            while (rset.next()) {
                friendId = rset.getInt("friend_id");
                if (friendId == ObjId) { continue; }

                list.add(friendId);
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Error found in {} friendlist while loading BlockList: {}", ObjId, e.getMessage(), e);
        }
        return list;
    }

    public static boolean isBlocked(L2PcInstance listOwner, L2PcInstance target) {
        BlockList blockList = listOwner.getBlockList();
        return blockList.isBlockAll() || blockList.isInBlockList(target);
    }

    public static boolean isBlocked(L2PcInstance listOwner, int targetId) {
        BlockList blockList = listOwner.getBlockList();
        return blockList.isBlockAll() || blockList.isInBlockList(targetId);
    }

    public static void addToBlockList(L2PcInstance listOwner, int targetId) {
        if (listOwner == null) { return; }

        String charName = CharNameTable.getInstance().getNameById(targetId);

        if (listOwner.getFriendList().contains(targetId)) {
            SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST);
            sm.addString(charName);
            listOwner.sendPacket(sm);
            return;
        }

        if (listOwner.getBlockList().getBlockList().contains(targetId)) {
            listOwner.sendMessage("Already in ignore list.");
            return;
        }

        listOwner.getBlockList().addToBlockList(targetId);

        SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_ADDED_TO_YOUR_IGNORE_LIST);
        sm.addString(charName);
        listOwner.sendPacket(sm);

        L2PcInstance player = L2World.getInstance().getPlayer(targetId);

        if (player != null) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
            sm.addString(listOwner.getName());
            player.sendPacket(sm);
        }
    }

    public static void removeFromBlockList(L2PcInstance listOwner, int targetId) {
        if (listOwner == null) { return; }

        SystemMessage sm;
        String charName = CharNameTable.getInstance().getNameById(targetId);

        if (!listOwner.getBlockList().getBlockList().contains(targetId)) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT);
            listOwner.sendPacket(sm);
            return;
        }

        listOwner.getBlockList().removeFromBlockList(targetId);

        sm = SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST);
        sm.addString(charName);
        listOwner.sendPacket(sm);
    }

    public static boolean isInBlockList(L2PcInstance listOwner, L2PcInstance target) {
        return listOwner.getBlockList().isInBlockList(target);
    }

    public static void setBlockAll(L2PcInstance listOwner, boolean newValue) {
        listOwner.getBlockList().setBlockAll(newValue);
    }

    public static void sendListToOwner(L2PcInstance listOwner) {
        int i = 1;
        listOwner.sendPacket(SystemMessageId.BLOCK_LIST_HEADER);

        for (int playerId : listOwner.getBlockList().getBlockList()) {
            listOwner.sendMessage((i++) + ". " + CharNameTable.getInstance().getNameById(playerId));
        }

        listOwner.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
    }

    /**
     * @param ownerId  object id of owner block list
     * @param targetId object id of potential blocked player
     * @return true if blocked
     */
    public static boolean isInBlockList(int ownerId, int targetId) {
        L2PcInstance player = L2World.getInstance().getPlayer(ownerId);

        if (player != null) { return BlockList.isBlocked(player, targetId); }

        if (!_offlineList.containsKey(ownerId)) { _offlineList.put(ownerId, loadList(ownerId)); }

        return _offlineList.get(ownerId).contains(targetId);
    }

    private synchronized void addToBlockList(int target) {
        _blockList.add(target);
        updateInDB(target, true);
    }

    private synchronized void removeFromBlockList(int target) {
        _blockList.remove(Integer.valueOf(target));
        updateInDB(target, false);
    }

    public void playerLogout() {
        _offlineList.put(_owner.getObjectId(), _blockList);
    }

    private void updateInDB(int targetId, boolean state) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement;

            if (state) {
                statement = con.prepareStatement("INSERT INTO character_friends (char_id, friend_id, relation) VALUES (?, ?, 1)");
                statement.setInt(1, _owner.getObjectId());
                statement.setInt(2, targetId);
            }
            else {
                statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id = ? AND friend_id = ? AND relation = 1");
                statement.setInt(1, _owner.getObjectId());
                statement.setInt(2, targetId);
            }
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Could not add/remove block player: {}", e.getMessage(), e);
        }
    }

    public boolean isInBlockList(L2PcInstance target) {
        return _blockList.contains(target.getObjectId());
    }

    public boolean isInBlockList(int targetId) {
        return _blockList.contains(targetId);
    }

    private boolean isBlockAll() {
        return _owner.isInRefusalMode();
    }

    private void setBlockAll(boolean state) {
        _owner.setInRefusalMode(state);
    }

    public List<Integer> getBlockList() {
        return _blockList;
    }

    public boolean isBlockAll(L2PcInstance listOwner) {
        return listOwner.getBlockList().isBlockAll();
    }
}