package net.sf.l2j.gameserver.model;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.entity.DimensionalRift;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class L2Party {
    public static final int ITEM_LOOTER = 0;
    public static final int ITEM_RANDOM = 1;
    public static final int ITEM_RANDOM_SPOIL = 2;
    public static final int ITEM_ORDER = 3;
    public static final int ITEM_ORDER_SPOIL = 4;

    private static final double[] BONUS_EXP_SP = {
            1,
            1.30,
            1.39,
            1.50,
            1.54,
            1.58,
            1.63,
            1.67,
            1.71
    };
    private static final int PARTY_POSITION_BROADCAST = 12000;
    private final List<L2PcInstance> _members = new CopyOnWriteArrayList<>();
    private final int _itemDistribution;
    protected PartyMemberPosition _positionPacket;
    private boolean _pendingInvitation;
    private long _pendingInviteTimeout;
    private int _partyLvl;
    private int _itemLastLoot;
    private L2CommandChannel _commandChannel;
    private DimensionalRift _dr;
    private Future<?> _positionBroadcastTask;
    private boolean _disbanding = false;

    /**
     * constructor ensures party has always one member - leader
     */
    public L2Party(L2PcInstance leader, int itemDistribution) {
        _members.add(leader);

        _partyLvl = leader.getLevel();
        _itemDistribution = itemDistribution;
    }

    private static List<L2PcInstance> getValidMembers(List<L2PcInstance> members, int topLvl) {
        final List<L2PcInstance> validMembers = new ArrayList<>();

        // Fixed LevelDiff cutoff point
        if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("level")) {
            for (L2PcInstance member : members) {
                if (topLvl - member.getLevel() <= Config.PARTY_XP_CUTOFF_LEVEL) { validMembers.add(member); }
            }
        }
        // Fixed MinPercentage cutoff point
        else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("percentage")) {
            int sqLevelSum = 0;
            for (L2PcInstance member : members) { sqLevelSum += (member.getLevel() * member.getLevel()); }

            for (L2PcInstance member : members) {
                int sqLevel = member.getLevel() * member.getLevel();
                if (sqLevel * 100 >= sqLevelSum * Config.PARTY_XP_CUTOFF_PERCENT) { validMembers.add(member); }
            }
        }
        // Automatic cutoff method
        else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("auto")) {
            int sqLevelSum = 0;
            for (L2PcInstance member : members) { sqLevelSum += (member.getLevel() * member.getLevel()); }

            int i = members.size() - 1;
            if (i < 1) { return members; }

            if (i >= BONUS_EXP_SP.length) { i = BONUS_EXP_SP.length - 1; }

            for (L2PcInstance member : members) {
                int sqLevel = member.getLevel() * member.getLevel();
                if (sqLevel >= sqLevelSum * (1 - 1 / (1 + BONUS_EXP_SP[i] - BONUS_EXP_SP[i - 1]))) {
                    validMembers.add(member);
                }
            }
        }
        return validMembers;
    }

    private static double getBaseExpSpBonus(int membersCount) {
        int i = membersCount - 1;
        if (i < 1) { return 1; }

        if (i >= BONUS_EXP_SP.length) { i = BONUS_EXP_SP.length - 1; }

        return BONUS_EXP_SP[i];
    }

    private static double getExpBonus(int membersCount) {
        // Not a valid party
        if (membersCount < 2) { return getBaseExpSpBonus(membersCount); }

        return getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_XP;
    }

    private static double getSpBonus(int membersCount) {
        // Not a valid party
        if (membersCount < 2) { return getBaseExpSpBonus(membersCount); }

        return getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_SP;
    }

    /**
     * returns number of party members
     *
     * @return
     */
    public int getMemberCount() {
        return _members.size();
    }

    /**
     * Check if another player can start invitation process
     *
     * @return boolean if party waits for invitation respond
     */
    public boolean getPendingInvitation() {
        return _pendingInvitation;
    }

    /**
     * set invitation process flag and store time for expiration happens when: player join party or player decline to join
     *
     * @param val
     */
    public void setPendingInvitation(boolean val) {
        _pendingInvitation = val;
        _pendingInviteTimeout = System.currentTimeMillis() + L2PcInstance.REQUEST_TIMEOUT * 1000;
    }

    /**
     * Check if player invitation is expired
     *
     * @return boolean if time is expired
     * @see L2PcInstance#isRequestExpired()
     */
    public boolean isInvitationRequestExpired() {
        return _pendingInviteTimeout <= System.currentTimeMillis();
    }

    /** returns all party members */
    public final List<L2PcInstance> getPartyMembers() {
        return _members;
    }

    /**
     * get random member from party
     *
     * @param ItemId
     * @param target
     * @return
     */
    private L2PcInstance getRandomMember(int ItemId, L2Character target) {
        List<L2PcInstance> availableMembers = new ArrayList<>();
        for (L2PcInstance member : _members) {
            if (member != null && member.getInventory().validateCapacityByItemId(ItemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true)) {
                availableMembers.add(member);
            }
        }

        if (!availableMembers.isEmpty()) { return Rnd.get(availableMembers); }

        return null;
    }

    /**
     * get next item looter
     *
     * @param ItemId
     * @param target
     * @return
     */
    private L2PcInstance getNextLooter(int ItemId, L2Character target) {
        for (int i = 0; i < getMemberCount(); i++) {
            if (++_itemLastLoot >= getMemberCount()) { _itemLastLoot = 0; }

            L2PcInstance member = _members.get(_itemLastLoot);
            if (member != null && member.getInventory().validateCapacityByItemId(ItemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true)) {
                return member;
            }
        }

        return null;
    }

    /**
     * get next item looter
     *
     * @param player
     * @param ItemId
     * @param spoil
     * @param target
     * @return
     */
    private L2PcInstance getActualLooter(L2PcInstance player, int ItemId, boolean spoil, L2Character target) {
        L2PcInstance looter = player;

        switch (_itemDistribution) {
            case ITEM_RANDOM:
                if (!spoil) { looter = getRandomMember(ItemId, target); }
                break;

            case ITEM_RANDOM_SPOIL:
                looter = getRandomMember(ItemId, target);
                break;

            case ITEM_ORDER:
                if (!spoil) { looter = getNextLooter(ItemId, target); }
                break;

            case ITEM_ORDER_SPOIL:
                looter = getNextLooter(ItemId, target);
                break;
        }

        if (looter == null) { looter = player; }

        return looter;
    }

    /**
     * @param player The player to make checks on.
     * @return true if player is party leader.
     */
    public boolean isLeader(L2PcInstance player) {
        return (getLeader().equals(player));
    }

    /**
     * @return the Object ID for the party leader to be used as a unique identifier of this party
     */
    public int getPartyLeaderOID() {
        return getLeader().getObjectId();
    }

    /**
     * Broadcasts packet to every party member.
     *
     * @param packet The packet to broadcast.
     */
    public void broadcastToPartyMembers(L2GameServerPacket packet) {
        for (L2PcInstance member : _members) {
            if (member != null) { member.sendPacket(packet); }
        }
    }

    public void broadcastToPartyMembersNewLeader() {
        final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addPcName(getLeader());
        for (L2PcInstance member : _members) {
            if (member != null) {
                member.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
                member.sendPacket(new PartySmallWindowAll(member, this));
                member.broadcastUserInfo();
                member.sendPacket(sm);
            }
        }
    }

    public void broadcastCSToPartyMembers(CreatureSay msg, L2PcInstance broadcaster) {
        for (L2PcInstance member : _members) {
            if (member != null && !BlockList.isBlocked(member, broadcaster)) { member.sendPacket(msg); }
        }
    }

    /**
     * Send a packet to all other L2PcInstance of the Party.
     *
     * @param player
     * @param msg
     */
    public void broadcastToPartyMembers(L2PcInstance player, L2GameServerPacket msg) {
        for (L2PcInstance member : _members) {
            if (member != null && !member.equals(player)) { member.sendPacket(msg); }
        }
    }

    /**
     * adds new member to party
     *
     * @param player
     */
    public void addPartyMember(L2PcInstance player) {
        if (_members.contains(player)) { return; }

        // Send new member party window for all members
        player.sendPacket(new PartySmallWindowAll(player, this));
        broadcastToPartyMembers(new PartySmallWindowAdd(player, this));

        // Send messages
        player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_JOINED_S1_PARTY).addPcName(getLeader()));
        broadcastToPartyMembers(SystemMessage.getSystemMessage(SystemMessageId.S1_JOINED_PARTY).addPcName(player));

        // Add player to party, adjust party level
        _members.add(player);
        if (player.getLevel() > _partyLvl) { _partyLvl = player.getLevel(); }

        // Update partySpelled
        for (L2PcInstance member : _members) {
            if (member != null) {
                member.updateEffectIcons(true); // update party icons only
                member.broadcastUserInfo();
            }
        }

        if (isInDimensionalRift()) { _dr.partyMemberInvited(); }

        // open the CCInformationwindow
        if (isInCommandChannel()) { player.sendPacket(ExOpenMPCC.STATIC_PACKET); }

        // activate position task
        if (_positionBroadcastTask == null) {
            _positionBroadcastTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PositionBroadcast(), PARTY_POSITION_BROADCAST / 2, PARTY_POSITION_BROADCAST);
        }
    }

    /**
     * Removes a party member using its name.
     *
     * @param name player the player to be removed from the party.
     * @param type the message type {@link MessageType}.
     */
    public void removePartyMember(String name, MessageType type) {
        removePartyMember(getPlayerByName(name), type);
    }

    /**
     * Removes a party member instance.
     *
     * @param player the player to be removed from the party.
     * @param type   the message type {@link MessageType}.
     */
    public void removePartyMember(L2PcInstance player, MessageType type) {
        if (!_members.contains(player)) { return; }

        final boolean isLeader = isLeader(player);
        if (!_disbanding) {
            if (_members.size() == 2 || (isLeader && !Config.ALT_LEAVE_PARTY_LEADER && type != MessageType.Disconnected)) {
                _disbanding = true;

                for (L2PcInstance member : _members) {
                    member.sendPacket(SystemMessageId.PARTY_DISPERSED);
                    removePartyMember(member, MessageType.None);
                }
                return;
            }
        }

        _members.remove(player);
        recalculatePartyLevel();

        if (player.isFestivalParticipant()) { SevenSignsFestival.getInstance().updateParticipants(player, this); }

        if (player.isInDuel()) { DuelManager.getInstance().onRemoveFromParty(player); }

        if (player.getFusionSkill() != null) { player.abortCast(); }

        for (L2Character character : player.getKnownList().getKnownType(L2Character.class)) {
            if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == player) {
                character.abortCast();
            }
        }

        if (type == MessageType.Expelled) {
            player.sendPacket(SystemMessageId.HAVE_BEEN_EXPELLED_FROM_PARTY);
            broadcastToPartyMembers(SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_EXPELLED_FROM_PARTY).addPcName(player));
        }
        else if (type == MessageType.Left || type == MessageType.Disconnected) {
            player.sendPacket(SystemMessageId.YOU_LEFT_PARTY);
            broadcastToPartyMembers(SystemMessage.getSystemMessage(SystemMessageId.S1_LEFT_PARTY).addPcName(player));
        }

        player.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
        player.setParty(null);

        broadcastToPartyMembers(new PartySmallWindowDelete(player));

        if (isInDimensionalRift()) { _dr.partyMemberExited(player); }

        // Close the CCInfoWindow
        if (isInCommandChannel()) { player.sendPacket(ExCloseMPCC.STATIC_PACKET); }

        if (isLeader && _members.size() > 1 && (Config.ALT_LEAVE_PARTY_LEADER || type == MessageType.Disconnected)) {
            broadcastToPartyMembersNewLeader();
        }
        else if (_members.size() == 1) {
            if (isInCommandChannel()) {
                // delete the whole command channel when the party who opened the channel is disbanded
                if (_commandChannel.getChannelLeader().equals(getLeader())) { _commandChannel.disbandChannel(); }
                else { _commandChannel.removeParty(this); }
            }

            if (getLeader() != null) {
                getLeader().setParty(null);
                if (getLeader().isInDuel()) { DuelManager.getInstance().onRemoveFromParty(getLeader()); }
            }

            if (_positionBroadcastTask != null) {
                _positionBroadcastTask.cancel(false);
                _positionBroadcastTask = null;
            }
            _members.clear();
        }
    }

    /**
     * Change party leader (used for string arguments)
     *
     * @param name
     */
    public void changePartyLeader(String name) {
        L2PcInstance player = getPlayerByName(name);

        if (player != null && !player.isInDuel()) {
            if (_members.contains(player)) {
                if (isLeader(player)) { player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF); }
                else {
                    // Swap party members
                    L2PcInstance temp = getLeader();
                    int p1 = _members.indexOf(player);

                    _members.set(0, player);
                    _members.set(p1, temp);

                    broadcastToPartyMembersNewLeader();

                    if (isInCommandChannel() && temp.equals(_commandChannel.getChannelLeader())) {
                        _commandChannel.setChannelLeader(getLeader());
                        _commandChannel.broadcastToChannelMembers(SystemMessage.getSystemMessage(SystemMessageId.COMMAND_CHANNEL_LEADER_NOW_S1).addPcName(_commandChannel.getChannelLeader()));
                    }

                    if (player.isInPartyMatchRoom()) {
                        PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(player);
                        room.changeLeader(player);
                    }
                }
            }
            else { player.sendPacket(SystemMessageId.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER); }
        }
    }

    /**
     * finds a player in the party by name
     *
     * @param name
     * @return
     */
    private L2PcInstance getPlayerByName(String name) {
        for (L2PcInstance member : _members) {
            if (member.getName().equalsIgnoreCase(name)) { return member; }
        }
        return null;
    }

    /**
     * distribute item(s) to party members
     *
     * @param player
     * @param item
     */
    public void distributeItem(L2PcInstance player, L2ItemInstance item) {
        if (item.getItemId() == 57) {
            distributeAdena(player, item.getCount(), player);
            ItemTable.getInstance().destroyItem("Party", item, player, null);
            return;
        }

        L2PcInstance target = getActualLooter(player, item.getItemId(), false, player);
        target.addItem("Party", item, player, true);

        // Send messages to other party members about reward
        if (item.getCount() > 1) {
            broadcastToPartyMembers(target, SystemMessage.getSystemMessage(SystemMessageId.S1_OBTAINED_S3_S2).addPcName(target).addItemName(item).addItemNumber(item.getCount()));
        }
        else if (item.getEnchantLevel() > 0) {
            broadcastToPartyMembers(target, SystemMessage.getSystemMessage(SystemMessageId.S1_OBTAINED_S2_S3).addPcName(target).addNumber(item.getEnchantLevel()).addItemName(item));
        }
        else {
            broadcastToPartyMembers(target, SystemMessage.getSystemMessage(SystemMessageId.S1_OBTAINED_S2).addPcName(target).addItemName(item));
        }
    }

    /**
     * distribute item(s) to party members
     *
     * @param player
     * @param item
     * @param spoil
     * @param target
     */
    public void distributeItem(L2PcInstance player, IntIntHolder item, boolean spoil, L2Attackable target) {
        if (item == null) { return; }

        if (item.getId() == 57) {
            distributeAdena(player, item.getValue(), target);
            return;
        }

        L2PcInstance looter = getActualLooter(player, item.getId(), spoil, target);
        looter.addItem(spoil ? "Sweep" : "Party", item.getId(), item.getValue(), player, true);

        // Send messages to other party members about reward
        SystemMessage msg;
        if (item.getValue() > 1) {
            msg = spoil ? SystemMessage.getSystemMessage(SystemMessageId.S1_SWEEPED_UP_S3_S2) : SystemMessage.getSystemMessage(SystemMessageId.S1_OBTAINED_S3_S2);
            msg.addPcName(looter);
            msg.addItemName(item.getId());
            msg.addItemNumber(item.getValue());
        }
        else {
            msg = spoil ? SystemMessage.getSystemMessage(SystemMessageId.S1_SWEEPED_UP_S2) : SystemMessage.getSystemMessage(SystemMessageId.S1_OBTAINED_S2);
            msg.addPcName(looter);
            msg.addItemName(item.getId());
        }
        broadcastToPartyMembers(looter, msg);
    }

    /**
     * Distribute adena to party members, according distance.
     *
     * @param player The player who picked.
     * @param adena  Amount of adenas.
     * @param target Target used for distance checks.
     */
    public void distributeAdena(L2PcInstance player, int adena, L2Character target) {
        List<L2PcInstance> toReward = new ArrayList<>(_members.size());
        for (L2PcInstance member : _members) {
            if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true) || member.getAdena() == Integer.MAX_VALUE) {
                continue;
            }

            toReward.add(member);
        }

        // Avoid divisions by 0.
        if (toReward.isEmpty()) { return; }

        final int count = adena / toReward.size();
        for (L2PcInstance member : toReward) { member.addAdena("Party", count, player, true); }
    }

    /**
     * Distribute Experience and SP rewards to L2PcInstance Party members in the known area of the last attacker.<BR>
     * <BR>
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Get the L2PcInstance owner of the L2SummonInstance (if necessary)</li> <li>Calculate the Experience and SP reward distribution rate</li> <li>Add Experience and SP to the L2PcInstance</li><BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR>
     * <BR>
     * Exception are L2PetInstances that leech from the owner's XP; they get the exp indirectly, via the owner's exp gain<BR>
     *
     * @param xpReward        The Experience reward to distribute
     * @param spReward        The SP reward to distribute
     * @param rewardedMembers The list of L2PcInstance to reward
     * @param topLvl
     */
    public void distributeXpAndSp(long xpReward, int spReward, List<L2PcInstance> rewardedMembers, int topLvl) {
        final List<L2PcInstance> validMembers = getValidMembers(rewardedMembers, topLvl);

        xpReward *= getExpBonus(validMembers.size());
        spReward *= getSpBonus(validMembers.size());

        int sqLevelSum = 0;
        for (L2PcInstance member : validMembers) { sqLevelSum += member.getLevel() * member.getLevel(); }

        // Go through the L2PcInstances and L2PetInstances (not L2SummonInstances) that must be rewarded
        for (L2PcInstance member : rewardedMembers) {
            if (member.isDead()) { continue; }

            // Calculate and add the EXP and SP reward to the member
            if (validMembers.contains(member)) {
                // The servitor penalty
                final float penalty = member.hasServitor() ? ((L2SummonInstance) member.getPet()).getExpPenalty() : 0;

                final double sqLevel = member.getLevel() * member.getLevel();
                final double preCalculation = (sqLevel / sqLevelSum) * (1 - penalty);

                final long xp = Math.round(xpReward * preCalculation);

                // Set new karma.
                member.updateKarmaLoss(xp);

                // Add the XP/SP points to the requested party member
                member.addExpAndSp(xp, (int) (spReward * preCalculation));
            }
            else { member.addExpAndSp(0, 0); }
        }
    }

    /**
     * refresh party level
     */
    public void recalculatePartyLevel() {
        int newLevel = 0;
        for (L2PcInstance member : _members) {
            if (member == null) {
                _members.remove(member);
                continue;
            }

            if (member.getLevel() > newLevel) { newLevel = member.getLevel(); }
        }
        _partyLvl = newLevel;
    }

    public int getLevel() {
        return _partyLvl;
    }

    public int getLootDistribution() {
        return _itemDistribution;
    }

    public boolean isInCommandChannel() {
        return _commandChannel != null;
    }

    public L2CommandChannel getCommandChannel() {
        return _commandChannel;
    }

    public void setCommandChannel(L2CommandChannel channel) {
        _commandChannel = channel;
    }

    public boolean isInDimensionalRift() {
        return _dr != null;
    }

    public DimensionalRift getDimensionalRift() {
        return _dr;
    }

    public void setDimensionalRift(DimensionalRift dr) {
        _dr = dr;
    }

    public L2PcInstance getLeader() {
        try {
            return _members.get(0);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * The message type send to the party members.
     */
    public enum MessageType {
        Expelled,
        Left,
        None,
        Disconnected
    }

    protected class PositionBroadcast implements Runnable {
        @Override
        public void run() {
            if (_positionPacket == null) { _positionPacket = new PartyMemberPosition(L2Party.this); }
            else { _positionPacket.reuse(L2Party.this); }

            broadcastToPartyMembers(_positionPacket);
        }
    }
}