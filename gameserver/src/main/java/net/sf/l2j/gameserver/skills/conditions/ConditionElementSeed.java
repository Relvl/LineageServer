package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.func.Env;
import net.sf.l2j.gameserver.skills.effects.EffectSeed;

public final class ConditionElementSeed extends ACondition {
    private static final int[] SEED_SKILLS = {1285, 1286, 1287};
    private final int[] requiredSeeds;

    public ConditionElementSeed(int... seeds) {
        requiredSeeds = seeds;
    }

    @Override
    public boolean testImpl(Env env) {
        int[] seeds = new int[3];
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = env.getCharacter().getFirstEffect(SEED_SKILLS[i]) instanceof EffectSeed ?
                    ((EffectSeed) env.getCharacter().getFirstEffect(SEED_SKILLS[i])).getPower() :
                    0;
            if (seeds[i] >= requiredSeeds[i]) {
                seeds[i] -= requiredSeeds[i];
            }
            else { return false; }
        }

        if (requiredSeeds[3] > 0) {
            int count = 0;
            for (int i = 0; i < seeds.length && count < requiredSeeds[3]; i++) {
                if (seeds[i] > 0) {
                    seeds[i]--;
                    count++;
                }
            }
            if (count < requiredSeeds[3]) { return false; }
        }

        if (requiredSeeds[4] > 0) {
            int count = 0;
            for (int i = 0; i < seeds.length && count < requiredSeeds[4]; i++) {
                count += seeds[i];
            }
            if (count < requiredSeeds[4]) { return false; }
        }

        return true;
    }
}