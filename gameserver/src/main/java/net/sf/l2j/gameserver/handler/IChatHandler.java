package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

@FunctionalInterface
public interface IChatHandler {
    void handleChat(EChatType type, L2PcInstance activeChar, String tellTarget, String text);
}
