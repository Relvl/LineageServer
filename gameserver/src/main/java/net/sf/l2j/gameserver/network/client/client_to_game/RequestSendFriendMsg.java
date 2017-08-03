package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.L2FriendSay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestSendFriendMsg extends L2GameClientPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestSendFriendMsg.class);

    private String _message;
    private String _reciever;

    @Override
    protected void readImpl() {
        _message = readS();
        _reciever = readS();
    }

    @Override
    protected void runImpl() {
        if (_message == null || _message.isEmpty() || _message.length() > 300) { return; }

        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) { return; }

        L2PcInstance targetPlayer = L2World.getInstance().getPlayer(_reciever);
        if (targetPlayer == null || !targetPlayer.getFriendList().contains(activeChar.getObjectId())) {
            activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
            return;
        }

        if (Config.LOG_CHAT) {
            LOGGER.info("CHAT >> FRIEND {} -> {}: {}", activeChar.getName(), _reciever, _message);
        }

        targetPlayer.sendPacket(new L2FriendSay(activeChar.getName(), _reciever, _message));
    }
}