package net.sf.l2j.gameserver.network.client.client_to_game.gm;

import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.client_to_game.L2GameClientPacket;

public final class RequestGmList extends L2GameClientPacket {
    @Override
    protected void readImpl() {
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }
        GmListTable.getInstance().sendListToPlayer(activeChar);
    }
}