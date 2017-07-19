package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.PledgeReceiveMemberInfo;

public final class RequestPledgeReorganizeMember extends L2GameClientPacket {
    private int _isMemberSelected;
    private String _memberName;
    private int _newPledgeType;
    private String _selectedMember;

    @Override
    protected void readImpl() {
        _isMemberSelected = readD();
        _memberName = readS();
        _newPledgeType = readD();
        _selectedMember = readS();
    }

    @Override
    protected void runImpl() {
        final L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        final L2Clan clan = activeChar.getClan();
        if (clan == null) { return; }

        if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_MANAGE_RANKS) != L2Clan.CP_CL_MANAGE_RANKS) { return; }

        final L2ClanMember member1 = clan.getClanMember(_memberName);

        if (_isMemberSelected == 0) {
            if (member1 != null) {
                activeChar.sendPacket(new PledgeReceiveMemberInfo(member1)); // client changes affiliation info even if it fails, so we have to fix it manually
            }
            return;
        }

        final L2ClanMember member2 = clan.getClanMember(_selectedMember);

        if (member1 == null || member1.getObjectId() == clan.getLeaderId() || member2 == null || member2.getObjectId() == clan.getLeaderId()) { return; }

        // Do not send sub pledge leaders to other pledges than main
        if (clan.isSubPledgeLeader(member1.getObjectId())) {
            activeChar.sendPacket(new PledgeReceiveMemberInfo(member1));
            return;
        }

        final int oldPledgeType = member1.getPledgeType();
        if (oldPledgeType == _newPledgeType) { return; }

        member1.setPledgeType(_newPledgeType);
        member2.setPledgeType(oldPledgeType);

        clan.broadcastClanStatus();
    }
}