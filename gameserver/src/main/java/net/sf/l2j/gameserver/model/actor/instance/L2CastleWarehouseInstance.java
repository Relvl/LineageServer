package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;

/**
 * @author l3x
 */
public class L2CastleWarehouseInstance extends L2WarehouseInstance {
    protected static final int COND_ALL_FALSE = 0;
    protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
    protected static final int COND_OWNER = 2;

    public L2CastleWarehouseInstance(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public boolean isWarehouse() {
        return true;
    }

    @Override
    public void showChatWindow(L2PcInstance player, int val) {
        player.sendPacket(ActionFailed.STATIC_PACKET);
        String filename = "data/html/castlewarehouse/castlewarehouse-no.htm";

        int condition = validateCondition(player);
        if (condition > COND_ALL_FALSE) {
            if (condition == COND_BUSY_BECAUSE_OF_SIEGE) { filename = "data/html/castlewarehouse/castlewarehouse-busy.htm"; }
            else if (condition == COND_OWNER) {
                if (val == 0) { filename = "data/html/castlewarehouse/castlewarehouse.htm"; }
                else { filename = "data/html/castlewarehouse/castlewarehouse-" + val + ".htm"; }
            }
        }

        final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", getObjectId());
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }

    protected int validateCondition(L2PcInstance player) {
        if (getCastle() != null && player.getClan() != null) {
            if (getCastle().getSiege().isInProgress()) { return COND_BUSY_BECAUSE_OF_SIEGE; }

            if (getCastle().getOwnerId() == player.getClanId()) { return COND_OWNER; }
        }
        return COND_ALL_FALSE;
    }
}