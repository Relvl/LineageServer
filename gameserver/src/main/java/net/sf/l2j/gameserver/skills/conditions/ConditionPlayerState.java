package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.func.Env;

public final class ConditionPlayerState extends ACondition {
    public enum PlayerState {
        RESTING,
        MOVING,
        RUNNING,
        RIDING,
        FLYING,
        BEHIND,
        FRONT,
        OLYMPIAD
    }

    private final PlayerState state;
    private final boolean required;

    public ConditionPlayerState(PlayerState state, boolean required) {
        this.state = state;
        this.required = required;
    }

    @Override
    public boolean testImpl(Env env) {
        L2Character character = env.getCharacter();
        L2PcInstance player = env.getPlayer();
        switch (state) {
            case RESTING:
                return (player == null) ? !required : player.isSitting() == required;
            case MOVING:
                return character.isMoving() == required;
            case RUNNING:
                return character.isMoving() == required && character.isRunning() == required;
            case RIDING:
                return character.isRiding() == required;
            case FLYING:
                return character.isFlying() == required;
            case BEHIND:
                return character.isBehindTarget() == required;
            case FRONT:
                return character.isInFrontOfTarget() == required;
            case OLYMPIAD:
                return (player == null) ? !required : player.isInOlympiadMode() == required;
        }
        return !required;
    }
}