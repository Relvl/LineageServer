package net.sf.l2j.gameserver.playerpart.contact;

import net.sf.l2j.commons.database.CallException;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.client.game_to_client.FriendList;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.playerpart.contact.PlayerContactsLoadCall.ContactRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Johnson / 12.08.2017
 */
public class ContactController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContactController.class);

    private final L2PcInstance player;
    private final Set<Integer> blockList = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Integer, String> friendList = new ConcurrentHashMap<>();

    /** Заблокированы все приватные сообщения, кроме друзей. Не сохраняется при релогине. */
    private boolean blockAll;
    /** Игрок автоматически отклоняет все предложения обмена. */
    private boolean tradeRefusal;

    public ContactController(L2PcInstance player) {
        this.player = player;

        try (PlayerContactsLoadCall call = new PlayerContactsLoadCall(player.getObjectId())) {
            call.execute();
            for (ContactRow row : call.getContacts()) {
                switch (EContactType.getByType(row.getContactType())) {
                    case FRIEND:
                        friendList.put(row.getContactId(), CharNameTable.getInstance().getNameById(row.getContactId()));
                        break;
                    case IGNORED:
                        blockList.add(row.getContactId());
                        break;
                    default:
                        break;
                }
            }
        }
        catch (CallException e) {
            LOGGER.error("Cannot load player contacts", e);
        }
    }

    // region FRIENDS
    public boolean isFriend(Integer objectId) { return friendList.containsKey(objectId); }

    public Map<Integer, String> getFriends() { return friendList; }

    public void addFriend(L2PcInstance otherPlayer) {
        if (isFriend(otherPlayer.getObjectId())) { return; }
        if (checkAndChangeContact(otherPlayer.getObjectId(), EContactType.FRIEND)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_JOINED_AS_FRIEND).addPcName(otherPlayer));
            otherPlayer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ADDED_TO_FRIENDS).addPcName(player));

            friendList.put(otherPlayer.getObjectId(), otherPlayer.getName());
            otherPlayer.getContactController().friendList.put(player.getObjectId(), player.getName());

            player.sendPacket(new FriendList(player));
            otherPlayer.sendPacket(new FriendList(player));

            otherPlayer.sendPacket(SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND);
        }
    }

    public void removeFriend(Integer objectId) {
        if (objectId == null || objectId <= 0 || !isFriend(objectId)) { return; }

        if (checkAndChangeContact(objectId, EContactType.REMOVE)) {
            L2PcInstance targetPlayer = L2World.getInstance().getPlayer(objectId);
            if (targetPlayer != null) {
                targetPlayer.getContactController().friendList.remove(player.getObjectId());
                targetPlayer.sendPacket(new FriendList(targetPlayer));
            }

            try (PlayerContactChangeCall call = new PlayerContactChangeCall(objectId, player.getObjectId(), EContactType.REMOVE)) {
                call.execute();
            }
            catch (CallException e) {
                LOGGER.error("Cannot remove player's friend contact", e);
            }

            String exFriendName = friendList.remove(objectId);
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST).addString(exFriendName));
            player.sendPacket(new FriendList(player));
        }
    }

    public void notifyFriends(boolean imOnline) {
        for (Integer friendObjectId : friendList.keySet()) {
            L2PcInstance friend = L2World.getInstance().getPlayer(friendObjectId);
            if (friend != null) {
                friend.sendPacket(new FriendList(friend));
                if (imOnline) {
                    friend.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN).addPcName(player));
                }
            }
        }
    }
    // endregion FRIENDS

    public boolean isBlocked(L2PcInstance target) { return blockList.contains(target.getObjectId()); }

    public boolean isBlocked(Integer objectId) { return blockList.contains(objectId); }

    public void blockPlayer(Integer objectId) {
        if (objectId == null || objectId <= 0) { return; }
        String charName = CharNameTable.getInstance().getNameById(objectId);
        if (isBlocked(objectId)) {
            player.sendMessage("Вы уже игнорируете " + charName);
            return;
        }
        if (isFriend(objectId)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST).addString(charName));
            return;
        }
        if (checkAndChangeContact(objectId, EContactType.IGNORED)) {
            blockList.add(objectId);
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_ADDED_TO_YOUR_IGNORE_LIST).addString(charName));

            L2PcInstance otherPlayer = L2World.getInstance().getPlayer(objectId);
            if (otherPlayer != null) {
                otherPlayer.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addPcName(player));
            }
        }
    }

    public void unblockPlayer(Integer objectId) {
        if (objectId == null || objectId <= 0) { return; }
        String charName = CharNameTable.getInstance().getNameById(objectId);
        if (!isBlocked(objectId)) {
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
            return;
        }
        if (checkAndChangeContact(objectId, EContactType.REMOVE)) {
            blockList.remove(objectId);
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST).addString(charName));
            L2PcInstance otherPlayer = L2World.getInstance().getPlayer(objectId);
            if (otherPlayer != null) {
                otherPlayer.sendMessage(player.getName() + " больше не игнорирует Вас.");
            }
        }
    }

    public void printBlockList() {
        player.sendPacket(SystemMessageId.BLOCK_LIST_HEADER);
        for (int playerId : blockList) {
            player.sendMessage(CharNameTable.getInstance().getNameById(playerId));
        }
        player.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
    }

    public boolean isBlockAll() { return blockAll; }

    public void setBlockAll(boolean blockAll) {
        this.blockAll = blockAll;
        player.sendPacket(blockAll ? SystemMessageId.MESSAGE_REFUSAL_MODE : SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
        player.sendPacket(new EtcStatusUpdate(player));
    }

    public boolean isTradeRefusal() { return tradeRefusal; }

    public void setTradeRefusal(boolean tradeRefusal) { this.tradeRefusal = tradeRefusal; }

    private boolean checkAndChangeContact(Integer contactId, EContactType contactType) {
        try (PlayerContactChangeCall call = new PlayerContactChangeCall(player.getObjectId(), contactId, contactType)) {
            call.execute();
            return true;
        }
        catch (CallException e) {
            LOGGER.error("Cannot store player contact", e);
            return false;
        }
    }
}
