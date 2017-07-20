package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.client.game_to_client.ShowTownMap;
import net.sf.l2j.gameserver.network.client.game_to_client.StaticObject;

public class L2StaticObjectInstance extends L2Object {
    private int staticObjectId;
    private int type = -1; // 0 - map signs, 1 - throne , 2 - arena signs
    private boolean isBusy; // True - if someone sitting on the throne
    private ShowTownMap townMap;

    public L2StaticObjectInstance(int objectId) {
        super(objectId);
    }

    public int getStaticObjectId() {
        return staticObjectId;
    }

    public void setStaticObjectId(int staticObjectId) { this.staticObjectId = staticObjectId; }

    public int getType() { return type; }

    public void setType(int type) { this.type = type; }

    public boolean isBusy() { return isBusy; }

    public void setBusy(boolean busy) { isBusy = busy; }

    public void setMap(String texture, int x, int y) {
        townMap = new ShowTownMap("town_map." + texture, x, y);
    }

    public ShowTownMap getMap() { return townMap; }

    @Override
    public void onAction(L2PcInstance player) {
        if (player.getTarget() == this) {
            if (!player.isInsideRadius(this, L2Npc.INTERACTION_DISTANCE, false, false)) {
                player.getAI().setIntention(EIntention.INTERACT, this);
            }
            else {
                if (type == 2) {
                    NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                    html.setFile("data/html/signboard.htm");
                    player.sendPacket(html);
                }
                else if (type == 0) {
                    player.sendPacket(townMap);
                }
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
        }
        else { player.setTarget(this); }
    }

    @Override
    public void onActionShift(L2PcInstance player) {
        if (player.isGM()) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile("data/html/admin/staticinfo.htm");
            html.replace("%x%", getX());
            html.replace("%y%", getY());
            html.replace("%z%", getZ());
            html.replace("%objid%", getObjectId());
            html.replace("%staticid%", staticObjectId);
            html.replace("%class%", getClass().getSimpleName());
            player.sendPacket(html);
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }

        if (player.getTarget() == this) { player.sendPacket(ActionFailed.STATIC_PACKET); }
        else { player.setTarget(this); }
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return false;
    }

    @Override
    public void sendInfo(L2PcInstance activeChar) {
        activeChar.sendPacket(new StaticObject(this));
    }
}