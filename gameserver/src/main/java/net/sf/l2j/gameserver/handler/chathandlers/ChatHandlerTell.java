package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class ChatHandlerTell implements IChatHandler {
    @Override
    public void handleChat(EChatType type, L2PcInstance activeChar, String tellTarget, String text) {
        if (tellTarget == null) { return; }

        L2PcInstance receiver = L2World.getInstance().getPlayer(tellTarget);
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

        if (!activeChar.isGM()) {
            if (receiver.getContactController().isBlockAll()) {
                activeChar.sendPacket(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
                return;
            }
            if (receiver.getContactController().isBlocked(activeChar)) {
                activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST2).addPcName(receiver));
                return;
            }
        }

        receiver.sendPacket(new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text));
        activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), type, "->" + receiver.getName(), text));
    }
}