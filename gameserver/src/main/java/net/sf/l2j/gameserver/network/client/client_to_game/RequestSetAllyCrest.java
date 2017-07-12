package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.CrestCache.CrestType;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public final class RequestSetAllyCrest extends L2GameClientPacket {
    private int _length;
    private byte[] _data;

    @Override
    protected void readImpl() {
        _length = readD();
        if (_length > 192) { return; }

        _data = new byte[_length];
        readB(_data);
    }

    @Override
    protected void runImpl() {
        final L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (_length < 0) {
            activeChar.sendMessage("File transfer error.");
            return;
        }

        if (_length > 192) {
            activeChar.sendMessage("The crest file size was too big (max 192 bytes).");
            return;
        }

        if (activeChar.getAllyId() != 0) {
            L2Clan leaderclan = ClanTable.getInstance().getClan(activeChar.getAllyId());
            if (activeChar.getClanId() != leaderclan.getClanId() || !activeChar.isClanLeader()) { return; }

            boolean remove = false;
            if (_length == 0 || _data.length == 0) { remove = true; }

            int newId = 0;
            if (!remove) { newId = IdFactory.getInstance().getNextId(); }

            if (!remove && !CrestCache.getInstance().saveCrest(CrestType.ALLY, newId, _data)) {
                _log.warn("Error saving crest for ally {} [{}]", leaderclan.getAllyName(), leaderclan.getAllyId());
                return;
            }

            leaderclan.changeAllyCrest(newId, false);
        }
    }
}