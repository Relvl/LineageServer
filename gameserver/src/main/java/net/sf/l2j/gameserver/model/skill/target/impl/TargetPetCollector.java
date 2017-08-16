package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

/**
 * Цель скилла - собственный пет (не саммон!), игнорируя текущую цель.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetPetCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        return activeChar.getPet() != null && !activeChar.getPet().isDead() ? new L2Summon[]{ activeChar.getPet() } : sendIncorrect(activeChar);
    }
}
