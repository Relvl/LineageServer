package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

/**
 * Цель скилла - собственный мертвый питомец, игнорируя текущую цель.
 * Только для игроков.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetPetCorpseCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        return activeChar.isPlayer() && activeChar.getPet() != null && activeChar.getPet().isDead() ?
                new L2Character[]{activeChar.getPet()} :
                sendIncorrect(activeChar);
    }
}
