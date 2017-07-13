package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClanWarsList implements IUserCommandHandler {
    private static final int[] COMMAND_IDS = {88, 89, 90};

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        L2Clan clan = activeChar.getClan();
        if (clan == null) {
            activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return false;
        }

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement;

            // Attack List
            if (id == 88) {
                statement = con.prepareStatement("SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan1=? AND clan_id=clan2 AND clan2 NOT IN (SELECT clan1 FROM clan_wars WHERE clan2=?)");
            }
            // Under Attack List
            else if (id == 89) {
                statement = con.prepareStatement("SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan2=? AND clan_id=clan1 AND clan1 NOT IN (SELECT clan2 FROM clan_wars WHERE clan1=?)");
            }
            // War List
            else {
                statement = con.prepareStatement("SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan1=? AND clan_id=clan2 AND clan2 IN (SELECT clan1 FROM clan_wars WHERE clan2=?)");
            }

            statement.setInt(1, clan.getClanId());
            statement.setInt(2, clan.getClanId());

            ResultSet rset = statement.executeQuery();

            if (rset.first()) {
                if (id == 88) { activeChar.sendPacket(SystemMessageId.CLANS_YOU_DECLARED_WAR_ON); }
                else if (id == 89) { activeChar.sendPacket(SystemMessageId.CLANS_THAT_HAVE_DECLARED_WAR_ON_YOU); }
                else { activeChar.sendPacket(SystemMessageId.WAR_LIST); }

                SystemMessage sm;
                while (rset.next()) {
                    String clanName = rset.getString("clan_name");

                    if (rset.getInt("ally_id") > 0) {
                        sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_ALLIANCE).addString(clanName).addString(rset.getString("ally_name"));
                    }
                    else { sm = SystemMessage.getSystemMessage(SystemMessageId.S1_NO_ALLI_EXISTS).addString(clanName); }

                    activeChar.sendPacket(sm);
                }

                activeChar.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
            }
            else {
                if (id == 88) { activeChar.sendPacket(SystemMessageId.YOU_ARENT_IN_CLAN_WARS); }
                else if (id == 89) { activeChar.sendPacket(SystemMessageId.NO_CLAN_WARS_VS_YOU); }
                else if (id == 90) { activeChar.sendPacket(SystemMessageId.NOT_INVOLVED_IN_WAR); }
            }

            rset.close();
            statement.close();
        } catch (Exception e) {
        }
        return true;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}