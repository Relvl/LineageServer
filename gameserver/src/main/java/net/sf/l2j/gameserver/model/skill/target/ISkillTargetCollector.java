package net.sf.l2j.gameserver.model.skill.target;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

/**
 * @author Johnson / 24.07.2017
 */
@FunctionalInterface
public interface ISkillTargetCollector {
    @SuppressWarnings("ConstantDeclaredInInterface")
    L2Object[] EMPTY_TARGET_LIST = new L2Object[0];

    L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill);

    default L2Object[] sendIncorrect(L2Character activeChar) {
        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
        return EMPTY_TARGET_LIST;
    }

    default boolean canAddSummon(L2Character caster, L2PcInstance owner, int radius, boolean isDead) {
        L2Summon summon = owner.getPet();
        return summon != null && canAddCharacter(caster, summon, radius, isDead);
    }

    default boolean canAddCharacter(L2Character caster, L2Character target, int radius, boolean isDead) {
        return isDead == target.isDead() && !(radius > 0 && !Util.checkIfInRange(radius, caster, target, true));
    }
}
