package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;

public class GatekeeperZombies extends AbstractNpcAI {
    private static final int VISITORS_MARK = 8064;
    private static final int FADED_VISITORS_MARK = 8065;
    private static final int PAGANS_MARK = 8067;

    public GatekeeperZombies() {
        super("ai/group");
        addAggroRangeEnterId(22136);
    }

    @Override
    public String onAggro(L2Npc npc, L2PcInstance player, boolean isPet) {
        if (player.getInventory().hasAtLeastOneItem(VISITORS_MARK, FADED_VISITORS_MARK, PAGANS_MARK)) {
            return null;
        }
        return super.onAggro(npc, player, isPet);
    }
}