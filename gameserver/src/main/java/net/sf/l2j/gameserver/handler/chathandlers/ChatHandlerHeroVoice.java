package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public class ChatHandlerHeroVoice implements IChatHandler {
    @Override
    public void handleChat(EChatType type, L2PcInstance activeChar, String params, String text) {
        if (!activeChar.isGM()) {
            if (!activeChar.isHero()) { return; }
            if (!FloodProtectors.performAction(activeChar.getClient(), Action.HERO_VOICE)) { return; }
        }
        CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
        for (L2PcInstance player : L2World.getInstance().getPlayers()) { player.sendPacket(cs); }
    }
}