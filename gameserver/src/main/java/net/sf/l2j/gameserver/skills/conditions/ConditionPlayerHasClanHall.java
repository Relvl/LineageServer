package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.skills.func.Env;

import java.util.List;

public final class ConditionPlayerHasClanHall extends ACondition {
    private final List<Integer> clanHall;

    public ConditionPlayerHasClanHall(List<Integer> clanHall) {
        this.clanHall = clanHall;
    }

    @Override
    public boolean testImpl(Env env) {
        if (env.getPlayer() == null) { return false; }
        L2Clan clan = env.getPlayer().getClan();
        if (clan == null) { return clanHall.size() == 1 && clanHall.get(0) == 0; }
        if (clanHall.size() == 1 && clanHall.get(0) == -1) { return clan.hasHideout(); }
        return clanHall.contains(clan.getHideoutId());
    }
}