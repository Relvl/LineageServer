package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionUsingItemType extends ACondition {
    private final int mask;

    public ConditionUsingItemType(int mask) {
        this.mask = mask;
    }

    @Override
    public boolean testImpl(Env env) {
        return env.getCharacter().isPlayer() && (mask & env.getPlayer().getInventory().getWornMask()) != 0;
    }
}