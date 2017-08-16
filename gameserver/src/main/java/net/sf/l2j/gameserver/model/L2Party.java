package net.sf.l2j.gameserver.model;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.entity.DimensionalRift;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.ItemConst;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

import java.util.ArrayList;
import java.util.Collection;
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

    private static final double[] BONUS_EXP_SP = { 1, 1.30, 1.39, 1.50, 1.54, 1.58, 1.63, 1.67, 1.71 };
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
    private boolean _disbanding;

    public L2Party(L2PcInstance leader, int itemDistribution) {
        _members.add(leader);
        _partyLvl = leader.getLevel();
        _itemDistribution = itemDistribution;
    }

    private static List<L2PcInstance> getValidMembers(List<L2PcInstance> members, int topLvl) {
        List<L2PcInstance> validMembers = new ArrayList<>();
        if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("level")) {
            for (L2PcInstance member : members) {
                if (topLvl - member.getLevel() <= Config.PARTY_XP_CUTOFF_LEVEL) {
                    validMembers.add(member);
                }
            }
        }
        else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("percentage")) {
            int sqLevelSum = 0;
            for (L2PcInstance member : members) {
                sqLevelSum += member.getLevel() * member.getLevel();
            }
            for (L2PcInstance member : members) {
                int sqLevel = member.getLevel() * member.getLevel();
                if (sqLevel * 100 >= sqLevelSum * Config.PARTY_XP_CUTOFF_PERCENT) { validMembers.add(member); }
            }
        }
        else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("auto")) {
            int sqLevelSum = 0;
            for (L2PcInstance member : members) {
                sqLevelSum += member.getLevel() * member.getLevel();
            }
            int i = members.size() - 1;
            if (i < 1) { return members; }
            if (i >= BONUS_EXP_SP.length) {
                i = BONUS_EXP_SP.length - 1;
            }
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
        if (i >= BONUS_EXP_SP.length) {
            i = BONUS_EXP_SP.length - 1;
        }
        return BONUS_EXP_SP[i];
    }

    private static double getExpBonus(int membersCount) {
        if (membersCount < 2) {
            return getBaseExpSpBonus(membersCount);
        }
        return getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_XP;
    }

    private static double getSpBonus(int membersCount) {
        if (membersCount < 2) {
            return getBaseExpSpBonus(membersCount);
        }
        return getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_SP;
    }

    public int getMemberCount() { return _members.size(); }

    public boolean getPendingInvitation() { return _pendingInvitation; }

    public void setPendingInvitation(boolean val) {
        _pendingInvitation = val;
        _pendingInviteTimeout = System.currentTimeMillis() + L2PcInstance.REQUEST_TIMEOUT * 1000;
    }

    public boolean isInvitationRequestExpired() { return _pendingInviteTimeout <= System.currentTimeMillis(); }

    public final List<L2PcInstance> getPartyMembers() { return _members; }

    private L2PcInstance getRandomMember(int itemId, L2Character target) {
        List<L2PcInstance> availableMembers = new ArrayList<>();
        for (L2PcInstance member : _members) {
            if (member != null && member.getInventory().validateCapacityByItemId(itemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true)) {
                availableMembers.add(member);
            }
        }
        if (!availableMembers.isEmpty()) {
            return Rnd.get(availableMembers);
        }
        return null;
    }

    private L2PcInstance getNextLooter(int itemId, L2Character target) {
        for (int i = 0; i < getMemberCount(); i++) {
            if (++_itemLastLoot >= getMemberCount()) {
                _itemLastLoot = 0;
            }
            L2PcInstance member = _members.get(_itemLastLoot);
            if (member != null && member.getInventory().validateCapacityByItemId(itemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true)) {
                return member;
            }
        }
        return null;
    }

    private L2PcInstance getActualLooter(L2PcInstance player, int itemId, boolean spoil, L2Character target) {
        L2PcInstance looter = player;
        switch (_itemDistribution) {
            case ITEM_RANDOM:
                if (!spoil) { looter = getRandomMember(itemId, target); }
                break;
            case ITEM_RANDOM_SPOIL:
                looter = getRandomMember(itemId, target);
                break;
            case ITEM_ORDER:
                if (!spoil) { looter = getNextLooter(itemId, target); }
                break;
            case ITEM_ORDER_SPOIL:
                looter = getNextLooter(itemId, target);
                break;
        }
        if (looter == null) { looter = player; }
        return looter;
    }

    public boolean isLeader(L2PcInstance player) { return getLeader().equals(player); }

    public int getPartyLeaderOID() { return getLeader().getObjectId(); }

    public void broadcastToPartyMembers(L2GameServerPacket packet) {
        for (L2PcInstance member : _members) {
            if (member != null) {
                member.sendPacket(packet);
            }
        }
    }

    public void broadcastToPartyMembersNewLeader() {
        SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addPcName(getLeader());
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
            if (member != null && !member.getContactController().isBlocked(broadcaster)) {
                member.sendPacket(msg);
            }
        }
    }

    public void broadcastToPartyMembers(L2PcInstance player, L2GameServerPacket msg) {
        for (L2PcInstance member : _members) {
            if (member != null && !member.equals(player)) {
                member.sendPacket(msg);
            }
        }
    }

    public void addPartyMember(L2PcInstance player) {
        if (_members.contains(player)) { return; }
        player.sendPacket(new PartySmallWindowAll(player, this));
        broadcastToPartyMembers(new PartySmallWindowAdd(player, this));
        player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_JOINED_S1_PARTY).addPcName(getLeader()));
        broadcastToPartyMembers(SystemMessage.getSystemMessage(SystemMessageId.S1_JOINED_PARTY).addPcName(player));

        _members.add(player);
        if (player.getLevel() > _partyLvl) {
            _partyLvl = player.getLevel();
        }

        for (L2PcInstance member : _members) {
            if (member != null) {
                member.updateEffectIcons(true); // update party icons only
                member.broadcastUserInfo();
            }
        }
        if (isInDimensionalRift()) {
            _dr.partyMemberInvited();
        }
        if (isInCommandChannel()) {
            player.sendPacket(ExOpenMPCC.STATIC_PACKET);
        }
        if (_positionBroadcastTask == null) {
            _positionBroadcastTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new PositionBroadcast(), PARTY_POSITION_BROADCAST / 2, PARTY_POSITION_BROADCAST);
        }
    }

    public void removePartyMember(String name, MessageType type) { removePartyMember(getPlayerByName(name), type); }

    public void removePartyMember(L2PcInstance player, MessageType type) {
        if (!_members.contains(player)) { return; }
        boolean isLeader = isLeader(player);
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
        if (isInCommandChannel()) { player.sendPacket(ExCloseMPCC.STATIC_PACKET); }

        if (isLeader && _members.size() > 1 && (Config.ALT_LEAVE_PARTY_LEADER || type == MessageType.Disconnected)) {
            broadcastToPartyMembersNewLeader();
        }
        else if (_members.size() == 1) {
            if (isInCommandChannel()) {
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

    public void changePartyLeader(String name) {
        L2PcInstance player = getPlayerByName(name);
        if (player != null && !player.isInDuel()) {
            if (_members.contains(player)) {
                if (isLeader(player)) {
                    player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF);
                }
                else {
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
            else {
                player.sendPacket(SystemMessageId.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER);
            }
        }
    }

    private L2PcInstance getPlayerByName(String name) {
        for (L2PcInstance member : _members) {
            if (member.getName().equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    public void distributeItem(L2PcInstance player, L2ItemInstance item) {
        if (item.getItemId() == ItemConst.ADENA_ID) {
            distributeAdena(player, item.getCount(), player);
            ItemTable.getInstance().destroyItem(EItemProcessPurpose.PARTY, item, player, null);
            return;
        }

        L2PcInstance target = getActualLooter(player, item.getItemId(), false, player);
        target.addItem(EItemProcessPurpose.PARTY, item, player, true);

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

    public void distributeItem(L2PcInstance player, IntIntHolder item, boolean spoil, L2Attackable target) {
        if (item == null) { return; }
        if (item.getId() == ItemConst.ADENA_ID) {
            distributeAdena(player, item.getValue(), target);
            return;
        }
        L2PcInstance looter = getActualLooter(player, item.getId(), spoil, target);
        looter.addItem(spoil ? EItemProcessPurpose.SWEEP : EItemProcessPurpose.PARTY, item.getId(), item.getValue(), player, true);
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

    public void distributeAdena(L2PcInstance player, int adena, L2Character target) {
        Collection<L2PcInstance> toReward = new ArrayList<>(_members.size());
        for (L2PcInstance member : _members) {
            if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true) || member.getAdena() == Integer.MAX_VALUE) {
                continue;
            }
            toReward.add(member);
        }
        if (toReward.isEmpty()) { return; }
        for (L2PcInstance member : toReward) {
            member.getInventory().addAdena(EItemProcessPurpose.PARTY, adena / toReward.size(), player, true);
        }
    }

    public void distributeXpAndSp(long xpReward, int spReward, List<L2PcInstance> rewardedMembers, int topLvl) {
        List<L2PcInstance> validMembers = getValidMembers(rewardedMembers, topLvl);
        xpReward *= getExpBonus(validMembers.size());
        spReward *= getSpBonus(validMembers.size());
        int sqLevelSum = 0;
        for (L2PcInstance member : validMembers) {
            sqLevelSum += member.getLevel() * member.getLevel();
        }
        for (L2PcInstance member : rewardedMembers) {
            if (member.isDead()) { continue; }
            if (validMembers.contains(member)) {
                float penalty = member.hasServitor() ? ((L2SummonInstance) member.getPet()).getExpPenalty() : 0;
                double sqLevel = member.getLevel() * member.getLevel();
                double preCalculation = (sqLevel / sqLevelSum) * (1 - penalty);
                long xp = Math.round(xpReward * preCalculation);
                member.updateKarmaLoss(xp);
                member.addExpAndSp(xp, (int) (spReward * preCalculation));
            }
            else {
                member.addExpAndSp(0, 0);
            }
        }
    }

    public void recalculatePartyLevel() {
        int newLevel = 0;
        for (L2PcInstance member : _members) {
            if (member == null) {
                _members.remove(member);
                continue;
            }
            if (member.getLevel() > newLevel) {
                newLevel = member.getLevel();
            }
        }
        _partyLvl = newLevel;
    }

    public int getLevel() { return _partyLvl; }

    public int getLootDistribution() { return _itemDistribution; }

    public boolean isInCommandChannel() { return _commandChannel != null; }

    public L2CommandChannel getCommandChannel() { return _commandChannel; }

    public void setCommandChannel(L2CommandChannel channel) { _commandChannel = channel; }

    public boolean isInDimensionalRift() { return _dr != null; }

    public DimensionalRift getDimensionalRift() { return _dr; }

    public void setDimensionalRift(DimensionalRift dr) { _dr = dr; }

    public L2PcInstance getLeader() {
        try {
            return _members.get(0);
        }
        catch (NoSuchElementException ignored) {
            return null;
        }
    }

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