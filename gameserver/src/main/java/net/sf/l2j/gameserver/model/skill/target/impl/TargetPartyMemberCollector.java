package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

/**
 * Цель скилла - сам, свой питомец или хозяин, член своей пати. Только не мертвые цели.
 *
 * @author Johnson / 24.07.2017
 */
@SuppressWarnings("ObjectEquality")
public class TargetPartyMemberCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target == null) { return sendIncorrect(activeChar); }
        if (!target.isDead()) {
            if (target == activeChar) { return new L2Character[]{ target }; }
            if (activeChar.isPlayer() && target.isSummon() && activeChar.getPet() == target) { return new L2Character[]{ target }; }
            if (activeChar.isSummon() && target.isPlayer() && activeChar == target.getPet()) { return new L2Character[]{ target }; }
            if (activeChar.isInParty() && target.isInParty() && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID()) { return new L2Character[]{ target }; }
        }
        return sendIncorrect(activeChar);
    }
}
