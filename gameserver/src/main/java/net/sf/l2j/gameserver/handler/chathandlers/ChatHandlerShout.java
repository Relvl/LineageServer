package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.client.game_to_client.CreatureSay;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public class ChatHandlerShout implements IChatHandler {
    @Override
    public void handleChat(EChatType type, L2PcInstance activeChar, String params, String text) {
        if (!FloodProtectors.performAction(activeChar.getClient(), Action.GLOBAL_CHAT)) {
            return;
        }

        CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
        int region = MapRegionTable.getMapRegion(activeChar.getX(), activeChar.getY());

        for (L2PcInstance player : L2World.getInstance().getPlayers()) {
            if (!BlockList.isBlocked(player, activeChar) && region == MapRegionTable.getMapRegion(player.getX(), player.getY())) {
                player.sendPacket(cs);
            }
        }
    }
}