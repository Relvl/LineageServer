package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public class ChatHandlerAll implements IChatHandler {
    @Override
    public void handleChat(EChatType type, L2PcInstance activeChar, String tellTarget, String text) {
        if (!FloodProtectors.performAction(activeChar.getClient(), Action.GLOBAL_CHAT)) { return; }

        CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
        for (L2PcInstance player : activeChar.getKnownList().getKnownTypeInRadius(L2PcInstance.class, 1250)) {
            if (!player.getContactController().isBlocked(activeChar)) {
                player.sendPacket(cs);
            }
        }
        activeChar.sendPacket(cs);
    }
}