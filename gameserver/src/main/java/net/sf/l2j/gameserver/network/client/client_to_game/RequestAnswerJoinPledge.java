package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.playerpart.variables.EPlayerVariableKey;

public final class RequestAnswerJoinPledge extends L2GameClientPacket {
    private int _answer;

    @Override
    protected void readImpl() {
        _answer = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        L2PcInstance requestor = activeChar.getRequest().getPartner();
        if (requestor == null) { return; }

        if (_answer == 0) {
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_NOT_RESPOND_TO_S1_CLAN_INVITATION).addPcName(requestor));
            requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DID_NOT_RESPOND_TO_CLAN_INVITATION).addPcName(activeChar));
        }
        else {
            if (!(requestor.getRequest().getRequestPacket() instanceof RequestJoinPledge)) {
                return; // hax
            }

            RequestJoinPledge requestPacket = (RequestJoinPledge) requestor.getRequest().getRequestPacket();
            L2Clan clan = requestor.getClan();

            // we must double check this cause during response time conditions can be changed, i.e. another player could join clan
            if (clan.checkClanJoinCondition(requestor, activeChar, requestPacket.getPledgeType())) {
                activeChar.sendPacket(new JoinPledge(requestor.getClanId()));

                activeChar.setPledgeType(requestPacket.getPledgeType());

                switch (requestPacket.getPledgeType()) {
                    case L2Clan.SUBUNIT_ACADEMY:
                        activeChar.setPowerGrade(9);
                        activeChar.variables().set(EPlayerVariableKey.LVL_JOINED_ACADEMY, activeChar.getLevel());
                        break;

                    case L2Clan.SUBUNIT_ROYAL1:
                    case L2Clan.SUBUNIT_ROYAL2:
                        activeChar.setPowerGrade(7);
                        break;

                    case L2Clan.SUBUNIT_KNIGHT1:
                    case L2Clan.SUBUNIT_KNIGHT2:
                    case L2Clan.SUBUNIT_KNIGHT3:
                    case L2Clan.SUBUNIT_KNIGHT4:
                        activeChar.setPowerGrade(8);
                        break;

                    default:
                        activeChar.setPowerGrade(6);
                }

                clan.addClanMember(activeChar);
                activeChar.setClanPrivileges(activeChar.getClan().getRankPrivs(activeChar.getPowerGrade()));

                activeChar.sendPacket(SystemMessageId.ENTERED_THE_CLAN);

                clan.broadcastToOtherOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_JOINED_CLAN).addPcName(activeChar), activeChar);
                clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(activeChar), activeChar);
                clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));

                // this activates the clan tab on the new member
                activeChar.sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), 0));
                for (SubPledge sp : activeChar.getClan().getAllSubPledges()) {
                    activeChar.sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), sp.getId()));
                }

                activeChar.variables().remove(EPlayerVariableKey.CLAN_JOIN_EXPIRY_TIME);
                activeChar.broadcastUserInfo();
            }
        }
        activeChar.getRequest().onRequestResponse();
    }
}