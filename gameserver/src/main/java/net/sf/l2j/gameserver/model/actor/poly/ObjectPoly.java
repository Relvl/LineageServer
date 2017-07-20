package net.sf.l2j.gameserver.model.actor.poly;

import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.client.game_to_client.UserInfo;

public class ObjectPoly {
    private final L2Object activeObject;
    private int polyId;
    private String polyType;
    private NpcTemplate npcTemplate;

    public ObjectPoly(L2Object activeObject) {
        this.activeObject = activeObject;
    }

    public boolean setPolyInfo(String polyType, String polyId) {
        int id = Integer.parseInt(polyId);
        if ("npc".equals(polyType)) {
            NpcTemplate template = NpcTable.getInstance().getTemplate(id);
            if (template == null) { return false; }

            npcTemplate = template;
        }

        this.polyId = id;
        this.polyType = polyType;

        activeObject.decayMe();
        activeObject.spawnMe(activeObject.getX(), activeObject.getY(), activeObject.getZ());

        if (activeObject.isPlayer()) {
            ((L2PcInstance) activeObject).sendPacket(new UserInfo(((L2PcInstance) activeObject)));
        }

        return true;
    }

    public final boolean isMorphed() { return polyType != null; }

    public final int getPolyId() { return polyId; }

    public final String getPolyType() { return polyType; }

    public final NpcTemplate getNpcTemplate() { return npcTemplate; }
}
