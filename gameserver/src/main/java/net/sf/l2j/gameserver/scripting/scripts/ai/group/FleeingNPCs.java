package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;

public class FleeingNPCs extends AbstractNpcAI {
    // Victims and elpies
    private final int[] npcId = {
            18150,
            18151,
            18152,
            18153,
            18154,
            18155,
            18156,
            18157,
            20432
    };

    public FleeingNPCs() {
        super("ai/group");
        for (int element : npcId) {
            addEventId(element, EventType.ON_ATTACK);
        }
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) {
        if (npc.getNpcId() >= 18150 && npc.getNpcId() <= 18157) {
            npc.getAI().setIntention(EIntention.MOVE_TO, new HeadedLocation(npc.getX() + Rnd.get(-40, 40), npc.getY() + Rnd.get(-40, 40), npc.getZ(), npc.getHeading()));
            npc.getAI().setIntention(EIntention.IDLE, null, null);
            return null;
        }
        if (npc.getNpcId() == 20432) {
            if (Rnd.get(3) == 2) { npc.getAI().setIntention(EIntention.MOVE_TO, new HeadedLocation(npc.getX() + Rnd.get(-200, 200), npc.getY() + Rnd.get(-200, 200), npc.getZ(), npc.getHeading())); }

            npc.getAI().setIntention(EIntention.IDLE, null, null);
            return null;
        }
        return super.onAttack(npc, attacker, damage, isPet);
    }
}