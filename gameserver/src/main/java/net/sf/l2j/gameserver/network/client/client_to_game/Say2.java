package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.regex.Pattern;

public final class Say2 extends L2GameClientPacket {
    private static final Logger CHAT_LOG = LoggerFactory.getLogger("CHAT_LOG");
    private static final Pattern CHAT_REMOVE_NEWLINE_PTN = Pattern.compile("\\\\n");

    private String text;
    private int chatTypeId;
    private EChatType chatType;
    private String tellTarget;

    @Override
    protected void readImpl() {
        text = readS();
        chatTypeId = readD();
        chatType = EChatType.getByCode(chatTypeId);
        tellTarget = (chatTypeId == EChatType.TELL.getCode()) ? readS() : null;
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        if (chatType == EChatType.UNKNOWN) {
            _log.warn("Say2: Invalid type: {} Player : {} text: {}", chatTypeId, activeChar.getName(), text);
            activeChar.logout(ActionFailed.STATIC_PACKET);
            return;
        }

        if (text.isEmpty()) {
            _log.warn("{}: sending empty text. Possible packet hack.", activeChar.getName());
            activeChar.logout(ActionFailed.STATIC_PACKET);
            return;
        }

        if (text.length() > 100) {
            return;
        }

        if (!activeChar.isGM()) {
            if (chatType == EChatType.ANNOUNCEMENT) {
                _log.warn("{} tried to use announcements without GM status.", activeChar.getName());
                return;
            }
            if (activeChar.isChatBanned() || activeChar.isInJail()) {
                activeChar.sendPacket(SystemMessageId.CHATTING_PROHIBITED);
                return;
            }
        }

        if (chatType == EChatType.PETITION_PLAYER && activeChar.isGM()) { chatType = EChatType.PETITION_GM; }

        try {
            MDC.put("CHAT_TYPE", chatType.getName());
            MDC.put("CHAT_PLAYER", activeChar.getName());
            if (chatType == EChatType.TELL) {
                CHAT_LOG.info(" -> {}: {}", tellTarget, text);
            }
            else {
                CHAT_LOG.info(text);
            }
        }
        finally {
            MDC.remove("CHAT_TYPE");
            MDC.remove("CHAT_PLAYER");
        }

        text = CHAT_REMOVE_NEWLINE_PTN.matcher(text).replaceAll("");

        if (chatType.getHandler() != null) {
            chatType.getHandler().handleChat(chatType, activeChar, tellTarget, text);
        }
        else {
            _log.warn("{} tried to use unregistred chathandler type: {}.", activeChar.getName(), chatType);
        }
    }

    @Override
    protected boolean triggersOnActionRequest() {
        return false;
    }
}