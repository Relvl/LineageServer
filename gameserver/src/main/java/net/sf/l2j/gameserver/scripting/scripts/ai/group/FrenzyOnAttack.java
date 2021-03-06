package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;

/**
 * Frenzy behavior, so far 5 types of orcs.<br>
 * Few others monsters got that skillid, need to investigate later :
 * <ul>
 * <li>Halisha's Officer</li>
 * <li>Executioner of Halisha</li>
 * <li>Alpine Kookaburra</li>
 * <li>Alpine Buffalo</li>
 * <li>Alpine Cougar</li>
 * </ul>
 *
 * @author Tryskell
 */
public class FrenzyOnAttack extends AbstractNpcAI {
    private static final L2Skill ULTIMATE_BUFF = SkillTable.getInfo(4318, 1);

    private static final String[] ORCS_WORDS =
            {
                    "Dear ultimate power!!!",
                    "The battle has just begun!",
                    "I never thought I'd use this against a novice!",
                    "You won't take me down easily."
            };

    public FrenzyOnAttack() {
        super("ai/group");
        addAttackId(20270, 20495, 20588, 20778, 21116);
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) {
        // The only requirements are HPs < 25% and not already under the buff. It's not 100% aswell.
        if (npc.getCurrentHp() / npc.getMaxHp() < 0.25 && npc.getFirstEffect(ULTIMATE_BUFF) == null && Rnd.get(10) == 0) {
            npc.broadcastNpcSay(ORCS_WORDS[Rnd.get(ORCS_WORDS.length)]);
            npc.setTarget(npc);
            npc.doCast(ULTIMATE_BUFF);
        }
        return super.onAttack(npc, attacker, damage, isPet);
    }
}