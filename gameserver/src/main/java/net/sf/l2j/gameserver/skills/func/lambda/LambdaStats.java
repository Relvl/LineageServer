package net.sf.l2j.gameserver.skills.func.lambda;

import net.sf.l2j.gameserver.skills.func.Env;

/** ¬озвращает значение запрошенного стата. */
public final class LambdaStats implements ILambda {
    public enum StatsType {
        PLAYER_LEVEL,
        TARGET_LEVEL,
        PLAYER_MAX_HP,
        PLAYER_MAX_MP
    }

    private final StatsType statsType;

    public LambdaStats(StatsType stat) {
        statsType = stat;
    }

    @Override
    public double calc(Env env) {
        switch (statsType) {
            case PLAYER_LEVEL:
                return (env.getCharacter() == null) ? 1 : env.getCharacter().getLevel();
            case TARGET_LEVEL:
                return (env.getTarget() == null) ? 1 : env.getTarget().getLevel();
            case PLAYER_MAX_HP:
                return (env.getCharacter() == null) ? 1 : env.getCharacter().getMaxHp();
            case PLAYER_MAX_MP:
                return (env.getCharacter() == null) ? 1 : env.getCharacter().getMaxMp();
        }
        return 0;
    }
}