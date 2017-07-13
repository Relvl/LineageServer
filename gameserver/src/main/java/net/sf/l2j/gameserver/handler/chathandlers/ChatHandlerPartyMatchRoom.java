package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;

public class ChatHandlerPartyMatchRoom implements IChatHandler {
    @Override
    public void handleChat(EChatType type, L2PcInstance activeChar, String tellTarget, String text) {
        if (!activeChar.isInPartyMatchRoom()) { return; }

        PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(activeChar);
        if (room == null) { return; }

        CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
        for (L2PcInstance member : room.getPartyMembers()) { member.sendPacket(cs); }
    }
}