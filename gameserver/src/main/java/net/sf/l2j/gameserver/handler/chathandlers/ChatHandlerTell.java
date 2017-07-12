package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;

public class ChatHandlerTell implements IChatHandler {
    @Override
    public void handleChat(EChatType type, L2PcInstance activeChar, String params, String text) {
        if (params == null) { return; }

        L2PcInstance receiver = L2World.getInstance().getPlayer(params);
        if (receiver == null || receiver.getClient().isDetached()) {
            activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
            return;
        }

        if (activeChar.equals(receiver)) {
            activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
            return;
        }

        if (receiver.isInJail() || receiver.isChatBanned()) {
            activeChar.sendPacket(SystemMessageId.TARGET_IS_CHAT_BANNED);
            return;
        }

        if (!activeChar.isGM() && (receiver.isInRefusalMode() || BlockList.isBlocked(receiver, activeChar))) {
            activeChar.sendPacket(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
            return;
        }

        receiver.sendPacket(new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text));
        activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), type, "->" + receiver.getName(), text));
    }
}