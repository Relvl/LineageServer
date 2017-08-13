package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.skills.func.Env;

import java.util.List;

public final class ConditionTargetRaceId extends ACondition {
    private final List<Integer> raceIds;

    public ConditionTargetRaceId(List<Integer> raceId) {
        raceIds = raceId;
    }

    @Override
    public boolean testImpl(Env env) {
        if (!(env.getTarget() instanceof L2Npc)) { return false; }
        return raceIds.contains(((L2Npc) env.getTarget()).getTemplate().getRace().ordinal());
    }
}