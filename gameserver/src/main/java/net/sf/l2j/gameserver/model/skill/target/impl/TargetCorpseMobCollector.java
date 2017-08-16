package net.sf.l2j.gameserver.model.skill.target.impl;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.target.ISkillTargetCollector;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * Цель скилла - мертвый моб.
 *
 * @author Johnson / 24.07.2017
 */
public class TargetCorpseMobCollector implements ISkillTargetCollector {
    @Override
    public L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (!(target instanceof L2Attackable) || !target.isDead()) {
            return sendIncorrect(activeChar);
        }
        if (skill.getSkillType() == L2SkillType.DRAIN && !DecayTaskManager.getInstance().isCorpseActionAllowed((L2Attackable) target)) {
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED));
            return EMPTY_TARGET_LIST;
        }
        return new L2Character[]{ target };
    }
}
