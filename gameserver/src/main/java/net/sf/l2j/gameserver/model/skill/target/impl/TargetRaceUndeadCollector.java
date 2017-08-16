package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

/**
 * Цель скилла - живая (ха-ха!) нежить (мобы и саммоны).
 *
 * @author Johnson / 24.07.2017
 */
public class TargetRaceUndeadCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target instanceof L2Npc || target instanceof L2SummonInstance) {
            if (!target.isUndead() || target.isDead()) {
                return sendIncorrect(activeChar);
            }
            return new L2Character[]{ target };
        }
        return sendIncorrect(activeChar);
    }
}
