package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;

/**
 * Цель скилла - другой игрок своей пати. Причем, если скилл FORCE - то только соответствующего типа (маг/немаг).
 *
 * @author Johnson / 24.07.2017
 */
@SuppressWarnings("ObjectEquality")
public class TargetPartyOtherMemberCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target == null || activeChar == target) { return sendIncorrect(activeChar); }
        if (!activeChar.isInParty() || !target.isInParty()) { return sendIncorrect(activeChar); }
        if (activeChar.getParty().getPartyLeaderOID() != target.getParty().getPartyLeaderOID()) { return sendIncorrect(activeChar); }
        if (target.isDead() || !target.isPlayer()) { return sendIncorrect(activeChar); }

        switch (skill.getId()) {
            // FORCE BUFFS may cancel here but there should be a proper condition
            case 426:
                if (!((L2PcInstance) target).isMageClass()) {
                    return new L2Character[]{ target };
                }
                return sendIncorrect(activeChar);
            case 427:
                if (((L2PcInstance) target).isMageClass()) {
                    return new L2Character[]{ target };
                }
                return sendIncorrect(activeChar);
        }

        return new L2Character[]{ target };
    }
}
