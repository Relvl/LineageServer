package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.base.PlayerRace;
import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerRace extends ACondition {
    private final PlayerRace _race;

    public ConditionPlayerRace(PlayerRace race) {
        _race = race;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getPlayer() != null && env.getPlayer().getRace() == _race;
    }
}