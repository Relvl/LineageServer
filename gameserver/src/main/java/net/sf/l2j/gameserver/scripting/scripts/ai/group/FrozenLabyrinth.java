package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;

/**
 * Frozen Labyrinth<br>
 * Those mobs split if you use physical attacks on them.
 *
 * @author malyelfik
 */
public final class FrozenLabyrinth extends AbstractNpcAI {
    // Monsters
    private static final int PRONGHORN_SPIRIT = 22087;
    private static final int PRONGHORN = 22088;
    private static final int LOST_BUFFALO = 22093;
    private static final int FROST_BUFFALO = 22094;

    public FrozenLabyrinth() {
        super("ai/group");

        addSkillSeeId(PRONGHORN, FROST_BUFFALO);
    }

    @Override
    public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet) {
        // Offensive physical skill casted on npc.
        if (skill != null && !skill.isMagic() && skill.isOffensive() && targets[0] == npc) {
            int spawnId = LOST_BUFFALO;
            if (npc.getNpcId() == PRONGHORN) { spawnId = PRONGHORN_SPIRIT; }

            int diff = 0;
            for (int i = 0; i < Rnd.get(6, 8); i++) {
                int x = diff < 60 ? npc.getX() + diff : npc.getX();
                int y = diff >= 60 ? npc.getY() + (diff - 40) : npc.getY();

                final L2Attackable monster = (L2Attackable) addSpawn(spawnId, x, y, npc.getZ(), npc.getHeading(), false, 120000, false);
                attack(monster, caster);
                diff += 20;
            }
            npc.deleteMe();
        }
        return super.onSkillSee(npc, caster, skill, targets, isPet);
    }
}