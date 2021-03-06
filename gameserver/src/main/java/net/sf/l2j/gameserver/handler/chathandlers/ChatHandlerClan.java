package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;

public class ChatHandlerClan implements IChatHandler {
    @Override
    public void handleChat(EChatType type, L2PcInstance activeChar, String tellTarget, String text) {
        if (activeChar.getClan() == null) {
            return;
        }
        activeChar.getClan().broadcastToOnlineMembers(new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text));
    }
}