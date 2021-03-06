package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.playerpart.variables.EPlayerVariableKey;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;

public class ClanPenalty implements IUserCommandHandler {
    private static final String NO_PENALTY = "<tr><td width=170>No penalty is imposed.</td><td width=100 align=center></td></tr>";

    private static final int[] COMMAND_IDS = { 100 };

    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();

        // Join a clan penalty.
        if (!activeChar.variables().isTimeInPast(EPlayerVariableKey.CLAN_JOIN_EXPIRY_TIME)) {
            StringUtil.append(sb,
                    "<tr><td width=170>Unable to join a clan.</td><td width=100 align=center>",
                    activeChar.variables().getLocalDateTime(EPlayerVariableKey.CLAN_JOIN_EXPIRY_TIME).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    "</td></tr>"
            );
        }

        // Create a clan penalty.
        if (!activeChar.variables().isTimeInPast(EPlayerVariableKey.CLAN_CREATE_EXPIRY_TIME)) {
            StringUtil.append(sb,
                    "<tr><td width=170>Unable to create a clan.</td><td width=100 align=center>",
                    activeChar.variables().getLocalDateTime(EPlayerVariableKey.CLAN_CREATE_EXPIRY_TIME).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    "</td></tr>"
            );
        }

        L2Clan clan = activeChar.getClan();
        if (clan != null) {
            // Invitation in a clan penalty.
            if (clan.getCharPenaltyExpiryTime() > System.currentTimeMillis()) {
                StringUtil.append(sb, "<tr><td width=170>Unable to invite a clan member.</td><td width=100 align=center>", sdf.format(clan.getCharPenaltyExpiryTime()), "</td></tr>");
            }

            // War penalty.
            if (!clan.getWarPenalty().isEmpty()) {
                for (Entry<Integer, Long> entry : clan.getWarPenalty().entrySet()) {
                    if (entry.getValue() > System.currentTimeMillis()) {
                        L2Clan enemyClan = ClanTable.getInstance().getClan(entry.getKey());
                        if (enemyClan != null) {
                            StringUtil.append(sb, "<tr><td width=170>Unable to attack ", enemyClan.getName(), " clan.</td><td width=100 align=center>", sdf.format(entry.getValue()), "</td></tr>");
                        }
                    }
                }
            }
        }

        NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/clan_penalty.htm");
        html.replace("%content%", (sb.length() == 0) ? NO_PENALTY : sb.toString());
        activeChar.sendPacket(html);
        return true;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}