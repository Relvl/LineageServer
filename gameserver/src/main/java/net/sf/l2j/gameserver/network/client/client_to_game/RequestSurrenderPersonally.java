package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.playerpart.variables.EPlayerVariableKey;

public final class RequestSurrenderPersonally extends L2GameClientPacket {
    private String pledgeName;

    @Override
    protected void readImpl() {
        pledgeName = readS();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }
        L2Clan playerClan = player.getClan();
        if (playerClan == null) { return; }

        L2Clan clan = ClanTable.getInstance().getClanByName(pledgeName);
        if (clan == null) { return; }

        if (!playerClan.isAtWarWith(clan.getClanId()) || player.variables().getBoolean(EPlayerVariableKey.WANTS_PEACE)) {
            player.sendPacket(SystemMessageId.FAILED_TO_PERSONALLY_SURRENDER);
            return;
        }

        player.variables().set(EPlayerVariableKey.WANTS_PEACE, true);
        player.deathPenalty(false, false, false);
        player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_PERSONALLY_SURRENDERED_TO_THE_S1_CLAN).addString(pledgeName));
        ClanTable.getInstance().checkSurrender(playerClan, clan);
    }
}