package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerHasCastle extends ACondition {
    private final int castle;

    public ConditionPlayerHasCastle(int castle) {
        this.castle = castle;
    }

    @Override
    public boolean testImpl(Env env) {
        if (env.getPlayer() == null) { return false; }
        L2Clan clan = env.getPlayer().getClan();
        if (clan == null) { return castle == 0; }
        if (castle == -1) { return clan.hasCastle(); }
        return clan.getCastleId() == castle;
    }
}