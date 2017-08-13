package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.skills.func.Env;

import java.util.List;

public final class ConditionTargetNpcId extends ACondition {
    private final List<Integer> npcIds;

    public ConditionTargetNpcId(List<Integer> npcIds) {
        this.npcIds = npcIds;
    }

    @Override
    public boolean testImpl(Env env) {
        if (env.getTarget() instanceof L2Npc) { return npcIds.contains(((L2Npc) env.getTarget()).getNpcId()); }
        if (env.getTarget() instanceof L2DoorInstance) { return npcIds.contains(((L2DoorInstance) env.getTarget()).getDoorId()); }
        return false;
    }
}