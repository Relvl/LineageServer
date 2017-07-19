package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.Henna;
import net.sf.l2j.gameserver.network.SystemMessageId;

public final class RequestHennaRemove extends L2GameClientPacket {
    private int symbolId;

    @Override
    protected void readImpl() {
        symbolId = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        for (int i = 1; i <= 3; i++) {
            Henna henna = activeChar.getHenna(i);
            if (henna != null && henna.getSymbolId() == symbolId) {
                if (activeChar.getAdena() >= (henna.getPrice() / 5)) {
                    activeChar.removeHenna(i);
                    break;
                }
                activeChar.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
            }
        }
    }
}