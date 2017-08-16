package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

/**
 * Цель скилла - единичная не мертвая цель, включая себя.
 *
 * @author Johnson / 24.07.2017
 */
@SuppressWarnings("ObjectEquality")
public class TargetOneCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target == null || target.isDead() || (target == activeChar && !skill.isSelfLegit())) {
            return sendIncorrect(activeChar);
        }
        return new L2Character[]{ target };
    }
}
