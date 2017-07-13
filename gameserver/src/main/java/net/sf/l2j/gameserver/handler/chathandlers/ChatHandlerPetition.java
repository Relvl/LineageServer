package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class ChatHandlerPetition implements IChatHandler {
    @Override
    public void handleChat(EChatType type, L2PcInstance activeChar, String tellTarget, String text) {
        if (!PetitionManager.getInstance().isPlayerInConsultation(activeChar)) {
            activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_PETITION_CHAT);
            return;
        }
        PetitionManager.getInstance().sendActivePetitionMessage(activeChar, text);
    }
}