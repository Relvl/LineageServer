package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;
import net.sf.l2j.gameserver.model.zone.ZoneId;

/**
 * Цель скилла - вражеский (война, пвп, пк, дуэль, оли) саммон.
 * TODO Думаю, должно распространяться и на пэтов (пусть плакают).
 *
 * @author Johnson / 24.07.2017
 */
public class TargetSummonEnemyCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target.isSummon()) {
            L2Summon targetSummon = (L2Summon) target;
            L2PcInstance summonOwner = targetSummon.getActingPlayer();
            if (activeChar instanceof L2PcInstance && activeChar.getPet() != targetSummon && !targetSummon.isDead() && (summonOwner.getPvpFlag() != 0 || summonOwner.getKarma() > 0) || (summonOwner.isInsideZone(ZoneId.PVP) && activeChar.isInsideZone(ZoneId.PVP)) || (summonOwner.isInDuel() && ((L2PcInstance) activeChar).isInDuel() && summonOwner.getDuelId() == ((L2PcInstance) activeChar).getDuelId())) {
                return new L2Character[]{targetSummon};
            }
        }
        return sendIncorrect(activeChar);
    }
}
